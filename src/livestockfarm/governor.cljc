(ns livestockfarm.governor
  "LivestockFarmGovernor — the independent safety/scope layer gating
  every farm scheduling/logistics proposal an advisor may make for a
  livestock-farm crew. The governor never dispatches hardware itself,
  never handles livestock on the farm floor itself, and never finalizes
  a livestock-handling-execution decision (e.g. deciding to approve a
  specific handling or restraint procedure) or a farm-safety-clearance
  decision (e.g. declaring a farm cleared for safety), and never
  overrides a farm safety supervisor's judgment — those are permanently
  out of this actor's scope and remain a farm safety supervisor's
  exclusive judgment (README's 'Robotics premise': this actor
  coordinates FARM SCHEDULING/LOGISTICS ONLY — it never handles
  livestock or makes farm-safety-clearance decisions itself). Modeled
  closely on cloud-itonami-isco-7511's butcher.governor for the animal-
  material-handling hazard-domain shape, extended with a second,
  independent outdoor-exposure/farm-equipment hazard-scope dimension
  (livestock farm labourers work with live animals in outdoor/uneven-
  terrain conditions under weather exposure, so animal-behavior-hazard
  and outdoor-exposure/equipment-condition stakes stack on top of the
  animal-handling hazard).

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. worker provenance     — the crew member must be independently
                                verified/registered before any action.
    2. farm provenance       — the farm must be independently verified/
                                registered before any action.
    3. no-actuation           — proposal :effect must be :propose (the
                                governor never dispatches hardware and
                                never handles livestock itself; it only
                                gates what the advisor may coordinate).
    4. closed op-allowlist    — only :log-work-record,
                                :schedule-crew-operation,
                                :flag-safety-concern and
                                :coordinate-supply-order may ever be
                                proposed; anything else is refused.
    5. scope-excluded action  — any proposal to directly finalize a
                                livestock-handling-execution decision
                                (e.g. approving a specific handling or
                                restraint procedure), or to directly
                                finalize a farm-safety-clearance decision
                                (e.g. declaring a farm cleared for
                                safety), or to override a farm safety
                                supervisor's judgment, is a hard,
                                permanent block (checked both against the
                                proposed :op and, defense-in-depth,
                                against the proposal's :rationale text —
                                matched as full finalization/execution
                                ACTION phrases such as \"approve the
                                livestock handling procedure\" /
                                \"declare the farm safety cleared\" /
                                \"override the farm safety supervisor's
                                judgment\", never as bare nouns like
                                \"livestock\", \"farm\" or \"safety\", so
                                the check can never self-trip on the
                                advisor's own routine rationale text,
                                e.g. \"logged work record for worker …\"
                                or \"scheduled crew operation for
                                livestock handling task …\" or \"…routed
                                for farm safety supervisor review\" — all
                                three legitimately contain those bare
                                nouns but none is a finalization action,
                                and all are exercised by
                                `governor-test/default-mock-advisor-proposals-never-self-trip-on-scope-exclusion`).
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off
  regardless of confidence):
    6. :op :flag-safety-concern (an animal-behavior-hazard /
                                outdoor-exposure / farm-equipment
                                concern always escalates to a human,
                                never auto-commits).
    7. :op :coordinate-supply-order above `supply-cost-threshold`.
    8. low confidence (< `confidence-floor`)."
  (:require [clojure.string :as str]
            [livestockfarm.store :as store]))

(def confidence-floor 0.6)
(def supply-cost-threshold 2000)

(def allowed-ops
  #{:log-work-record :schedule-crew-operation
    :flag-safety-concern :coordinate-supply-order})

;; Defense-in-depth: none of these ops are ever in `allowed-ops` above,
;; so they are already refused by the closed-allowlist check below; they
;; are named again here — as explicit finalization/execution ACTIONS,
;; never bare nouns — so a future allowlist edit cannot silently re-open
;; either of these two independent out-of-scope paths (livestock-
;; handling-execution finalization, farm-safety-clearance finalization)
;; without also touching this list.
(def ^:private scope-excluded-ops
  #{:finalize-livestock-handling-procedure :approve-livestock-handling-procedure
    :approve-restraint-procedure :finalize-livestock-handling-execution-decision
    :declare-farm-safety-cleared :finalize-farm-safety-clearance
    :clear-farm-for-safety
    :override-safety-supervisor-judgment :override-farm-safety-supervisor-judgment})

;; Full finalization/execution ACTION phrases only — never bare nouns
;; ("livestock", "handling", "restraint", "farm", "safety", "farm safety
;; supervisor") — so this can never match inside the mock advisor's own
;; default rationale text (which legitimately contains those bare
;; nouns, e.g. "livestock handling task" / "farm safety supervisor
;; review"). See
;; `governor-test/default-mock-advisor-proposals-never-self-trip-on-scope-exclusion`.
(def ^:private scope-excluded-phrases
  ["approve the livestock handling procedure" "approve the restraint procedure"
   "finalize the livestock handling procedure" "finalize the restraint procedure"
   "finalize the handling execution decision"
   "declare the farm safety cleared" "declare the farm cleared for safety"
   "finalize the farm safety clearance" "clear the farm for safety"
   "override the farm safety supervisor's judgment"
   "override the safety supervisor's judgment"
   "override farm safety supervisor judgment"
   "override safety supervisor judgment"])

(defn- contains-excluded-phrase? [s]
  (let [s (str/lower-case (or s ""))]
    (boolean (some #(str/includes? s %) scope-excluded-phrases))))

(defn- hard-violations [proposal worker-record farm-record]
  (let [{:keys [op rationale]} proposal]
    (cond-> []
      (nil? worker-record)
      (conj {:rule :no-worker
             :detail "未登録 worker への提案は不可（worker record は独立して検証・登録済みでなければならない）"})

      (nil? farm-record)
      (conj {:rule :no-farm
             :detail "未登録 farm への提案は不可（farm record は独立して検証・登録済みでなければならない）"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation
             :detail "effect は :propose のみ許可（governor は現場作業を直接実行しない）"})

      (not (contains? allowed-ops op))
      (conj {:rule :unknown-op
             :detail (str op " は closed op-allowlist に無い — 提案不可")})

      (or (contains? scope-excluded-ops op) (contains-excluded-phrase? rationale))
      (conj {:rule :scope-excluded-action
             :detail "家畜取扱い実行判断（取扱い・保定手順の承認を含む）の確定、farm safety clearance 判断の確定、farm safety supervisor の判断の上書きは、この actor の権限外 — 常に永続ブロック"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a `store`
  implementing `livestockfarm.store/Store`. Pure — never mutates the
  store, never dispatches a farm-floor operation, never finalizes a
  farm-safety-clearance decision."
  [request _context proposal store]
  (let [worker-record (store/worker store (:worker-id request))
        farm-record (some->> (:farm-id proposal) (store/farm store))
        hard (hard-violations proposal worker-record farm-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        supply-order-over-threshold?
        (and (= :coordinate-supply-order (:op proposal))
             (number? (:cost proposal))
             (> (:cost proposal) supply-cost-threshold))
        always-risky? (or (= :flag-safety-concern (:op proposal))
                           supply-order-over-threshold?)]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
