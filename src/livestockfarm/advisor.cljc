(ns livestockfarm.advisor
  "Livestock Farm Labourer Advisor — proposing a farm scheduling/logistics
  coordination operation (log a feeding/care-log/progress record, schedule
  a crew operation, flag a safety concern, coordinate a feed/farm-supplies
  procurement order) from a crew roster, farm registration and safety-
  reporting policy. Swappable mock/llm; the advisor ONLY proposes —
  `livestockfarm.governor` independently gates every proposal and always
  escalates safety concerns and above-threshold supply orders. The
  advisor never proposes to directly finalize a livestock-handling-
  execution decision (e.g. approving a specific handling/restraint
  procedure) or a farm-safety-clearance decision (e.g. declaring a farm
  cleared for safety), and never proposes to override a farm safety
  supervisor's judgment — those stay permanently out of this actor's
  scope. Modeled closely on cloud-itonami-isco-7511's butcher.advisor for
  the animal-material-handling hazard-domain shape, extended with a
  second, independent outdoor-exposure/farm-equipment hazard-scope
  dimension.

  A proposal: {:op :log-work-record|:schedule-crew-operation|
               :flag-safety-concern|:coordinate-supply-order
               :effect :propose :worker-id str :farm-id str
               :cost number :hazard-type kw :task str :stake kw
               :confidence n :rationale str}"
  (:require #?(:clj [clojure.edn :as edn] :cljs [cljs.reader :as edn])))

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- rationale-for [op worker-id farm-id hazard-type]
  (case op
    :log-work-record
    (str "logged work record for worker " worker-id " at farm " farm-id)

    :schedule-crew-operation
    (str "scheduled crew operation for livestock handling task at farm " farm-id)

    :flag-safety-concern
    (str "flagged " (name (or hazard-type :hazard)) " concern for worker "
         worker-id " at farm " farm-id " — routed for farm safety supervisor review")

    :coordinate-supply-order
    (str "coordinated supply order for worker " worker-id " at farm " farm-id)

    (str "proposed " (name op) " for worker " worker-id " at farm " farm-id)))

(defn- infer [_store {:keys [op stake worker-id farm-id cost hazard-type task]
                       :as request}]
  {:op op
   :effect :propose
   :worker-id worker-id
   :farm-id farm-id
   :cost cost
   :hazard-type hazard-type
   :task task
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (rationale-for op worker-id farm-id hazard-type)})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a livestock-farm scheduling/logistics coordination advisor.
   Given a request, propose an :op (one of :log-work-record,
   :schedule-crew-operation, :flag-safety-concern,
   :coordinate-supply-order), the :worker-id, :farm-id, and any
   :cost/:hazard-type/:task fields, an honest :confidence and a
   :stake. Never propose an op outside this closed list, and never
   propose to directly finalize a livestock-handling-execution decision
   (e.g. approving a specific handling or restraint procedure), or a
   farm-safety-clearance decision (e.g. declaring a farm cleared for
   safety), or to override a farm safety supervisor's judgment — those
   are always out of this actor's scope; it coordinates farm scheduling/
   logistics only and never handles livestock itself or makes farm-
   safety-clearance decisions itself. Safety concerns always require
   human sign-off regardless of confidence.")

(defn- parse-proposal [content]
  (try
    (let [p (edn/read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
