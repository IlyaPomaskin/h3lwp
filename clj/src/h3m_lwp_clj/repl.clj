(ns h3m-lwp-clj.repl
  (:import [com.badlogic.gdx Gdx])
  (:require
   [nrepl.misc :refer (response-for)]
   [nrepl.transport :as t]))
   

(defmacro post-runnable-wrapper
  [body]
  `(.postRunnable
    Gdx/app
    (proxy [Runnable] []
      (run []
        (try
          ~body
          (catch
           Exception
           e#
            (do
              (println (str "repl exception: " (.getMessage e#)))
              (println (.printStackTrace e#)))))))))


(defonce session-id (atom nil))


(defn wrap-code
  [msg]
  (update
   msg
   :code
   #(clojure.string/join
     ["(h3m-lwp-clj.repl/post-runnable-wrapper " % ")"])))


(defn should-toggle?
  [msg]
  (and (nil? @session-id)
       (= (:op msg) "eval")
       (= (:code msg) ":start-repl-wrap")))


(defn toggle-wrapper
  [msg]
  (reset! session-id (:session msg))
  (t/send
   (:transport msg)
   (response-for msg :status :done :out "Libgdx wrapper started")))


(defn mw
  [next-fn]
  (fn [msg]
    (if (and (some? @session-id)
             (= @session-id (:session msg)))
      (next-fn (wrap-code msg))
      (if (should-toggle? msg)
        (toggle-wrapper msg)
        (next-fn msg)))))

; :start-repl-wrap
