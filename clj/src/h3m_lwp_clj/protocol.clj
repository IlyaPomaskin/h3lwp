(ns h3m-lwp-clj.protocol)


(defprotocol Renderer
  (start [this])
  (stop [this])
  (render [this]))