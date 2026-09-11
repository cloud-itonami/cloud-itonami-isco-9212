(ns livestockfarm.store
  "SSoT for the ISCO-08 9212 livestock-farm scheduling/logistics
  coordination actor (itonami actor pattern, ADR-2607121000 / CLAUDE.md
  Actors section; README's 'Robotics premise' — a farm scheduling/logistics
  coordination robot performs crew scheduling, feeding/care-log/progress-
  record logging and feed/farm-supplies procurement coordination for a
  livestock farm crew under this advisor/governor pair, which never
  dispatches hardware itself, never handles livestock itself, and never
  finalizes a livestock-handling-execution decision or a farm-safety-
  clearance decision, and never overrides a farm safety supervisor's
  judgment — those remain the farm safety supervisor's exclusive
  judgment). Modeled closely on cloud-itonami-isco-7511's butcher.store
  for the animal-material-handling hazard-domain shape, extended with a
  second, independent outdoor-exposure/farm-equipment hazard-scope
  dimension.

  Domain:

    worker — a registered livestock-farm crew member
             (:worker-id, :name)
    farm   — a registered livestock farm {:farm-id :name
             :max-supply-cost number}. `:max-supply-cost` is an
             informational registered ceiling used only to decide whether
             a `:coordinate-supply-order` proposal escalates to human
             sign-off (the governor never blocks a within-threshold order
             outright; it only decides commit vs. escalate).
    record — a committed operating record (a logged feeding/care-log/
             progress entry, a scheduled crew operation, a flagged safety
             concern, or a coordinated supply order) — written ONLY via
             commit-record!.
    ledger — append-only audit trail, commit or hold.")

(defprotocol Store
  (worker [s worker-id])
  (farm [s farm-id])
  (records-of [s worker-id])
  (ledger [s])
  (register-worker! [s worker])
  (register-farm! [s farm])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (worker [_ worker-id] (get-in @a [:workers worker-id]))
  (farm [_ farm-id] (get-in @a [:farms farm-id]))
  (records-of [_ worker-id] (filter #(= worker-id (:worker-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-worker! [s w]
    (swap! a assoc-in [:workers (:worker-id w)] w) s)
  (register-farm! [s f]
    (swap! a assoc-in [:farms (:farm-id f)] f) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:workers {} :farms {} :records [] :ledger []}
                                    seed)))))
