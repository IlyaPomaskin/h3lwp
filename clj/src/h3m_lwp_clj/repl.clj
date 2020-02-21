(ns h3m-lwp-clj.repl)

(defmacro repl
  [body]
  `(.postRunnable
    Gdx/app
    (reify Runnable
      (run [_]
        (try
          ~body
          (catch
           Exception
           e#
            (do
              (println (str "repl exception: " (.getMessage e#)))
              (println (.printStackTrace e#)))))))))