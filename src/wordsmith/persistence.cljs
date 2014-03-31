(ns wordsmith.persistence)

(def prefix "wordsmith:documents:")

(def prefix-length (count prefix))

(defn generate-key [title]
  (str prefix title))

(defn set-document [title content]
  (let [k (generate-key title)]
    (.setItem js/localStorage k content)))

(defn get-document [title]
  (let [k (generate-key title)]
    (.getItem js/localStorage k)))

(defn remove-document [title]
  (let [k (generate-key title)]
    (.removeItem js/localStorage k)))

(defn get-all-titles []
  (let [ks (.keys js/Object js/localStorage)
        filtered (filter #(= (.substring % 0 prefix-length) prefix) ks)]
    (mapv #(.substring % prefix-length) filtered)))
