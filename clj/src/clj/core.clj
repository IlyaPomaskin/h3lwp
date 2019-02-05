(ns clj.core
  (:gen-class
   :prefix "method-"
   :methods [[getStringWithPrefix [String] String]]))

(defn method-getStringWithPrefix [this asdf]
      (str "prefix " asdf))
