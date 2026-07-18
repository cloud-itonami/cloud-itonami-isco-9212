(ns livestockfarm.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [livestockfarm.actor :as actor]
            [livestockfarm.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-worker! st {:worker-id "worker-1" :name "Kenji Sato"})
    (store/register-farm! st {:farm-id "FARM-1" :name "Sato Livestock Farm" :max-supply-cost 2000})
    st))

(deftest commits-a-registered-work-log
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:worker-id "worker-1" :op :log-work-record :stake :low
                  :farm-id "FARM-1" :task "feeding progress log"}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "worker-1"))))))

(deftest holds-an-unregistered-farm-proposal
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:worker-id "worker-1" :op :log-work-record :stake :low
                  :farm-id "FARM-ghost" :task "feeding progress log"}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :hold (:disposition (:state result))))
    (is (empty? (store/records-of st "worker-1")))))

(deftest interrupts-then-approves-safety-concern-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:worker-id "worker-1" :op :flag-safety-concern :stake :low
                  :farm-id "FARM-1" :hazard-type :animal-behavior-hazard}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "worker-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "worker-1")))))))

(deftest holds-a-scope-excluded-handling-op-even-at-high-confidence
  (testing "an actor run can never commit a proposal that would finalize a livestock-handling-execution decision, regardless of disposition path"
    (let [st (fresh-store)
          graph (actor/build-graph {:store st})
          request {:worker-id "worker-1" :op :approve-livestock-handling-procedure :stake :low
                    :farm-id "FARM-1" :task "handling decision"}
          result (actor/run-request! graph request {} "thread-4")]
      (is (= :done (:status result)))
      (is (= :hold (:disposition (:state result))))
      (is (empty? (store/records-of st "worker-1"))))))

(deftest holds-a-scope-excluded-farm-safety-clearance-op-even-at-high-confidence
  (testing "an actor run can never commit a proposal that would finalize a farm-safety-clearance decision (e.g. declaring a farm cleared for safety), regardless of disposition path"
    (let [st (fresh-store)
          graph (actor/build-graph {:store st})
          request {:worker-id "worker-1" :op :declare-farm-safety-cleared :stake :low
                    :farm-id "FARM-1" :task "farm safety clearance"}
          result (actor/run-request! graph request {} "thread-5")]
      (is (= :done (:status result)))
      (is (= :hold (:disposition (:state result))))
      (is (empty? (store/records-of st "worker-1"))))))
