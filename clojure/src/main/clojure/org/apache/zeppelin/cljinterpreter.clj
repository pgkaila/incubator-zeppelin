;; Licensed to the Apache Software Foundation (ASF) under one or more
;; contributor license agreements.  See the NOTICE file distributed with
;; this work for additional information regarding copyright ownership.
;; The ASF licenses this file to You under the Apache License, Version 2.0
;; (the "License"); you may not use this file except in compliance with
;; the License.  You may obtain a copy of the License at
;;
;;  http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns org.apache.zeppelin.cljinterpreter
  (:require [clojure.tools.nrepl.server :as server]
            [clojure.tools.nrepl :as client])
  (:import [org.apache.zeppelin.interpreter InterpreterResult InterpreterResult$Code InterpreterContext]
           (org.slf4j LoggerFactory Logger))
  (:gen-class
    :methods [^:static [open [] Object]
              ^:static [close [Object] void]
              ^:static [interpret [Object String org.apache.zeppelin.interpreter.InterpreterContext] org.apache.zeppelin.interpreter.InterpreterResult]]))

(def nrepl-server (atom nil))

(def logger ^Logger (LoggerFactory/getLogger "clojureLogger"))

(defn open []
  (do (swap! nrepl-server
             (fn [server?]
               (if server?
                 server?
                 (let [new-server (server/start-server)]
                   (client/client (client/connect :port (:port new-server)) 1000)))))))

(defn -open [] (open))

(defn close [connection]
  (.close connection))

(defn -close [connection]
  (close connection))

(defn ^InterpreterResult interpret
  [connection
   ^String cmd
   ^InterpreterContext context]

  (let [outputs (client/message connection {:op "eval" :code cmd})
        result (reduce #(merge %1 (select-keys %2 [:ex :err :value])) {} outputs)
       ]
    (.info logger (str "Clojure logs. output:" (pr-str outputs)))
    (if (result :err)
      (InterpreterResult. InterpreterResult$Code/ERROR (str "Exception: " (result :ex) "\nError: " (result :err)))
      (InterpreterResult. InterpreterResult$Code/SUCCESS (result :value))
    )
  )
)

(defn ^InterpreterResult -interpret
  [connection
   ^String cmd
   ^InterpreterContext context]
  (interpret connection cmd context))
