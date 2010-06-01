(ns idk.core
  (:use stupiddb.core
        [hiccup core form-helpers page-helpers]
        ring.adapter.jetty
        [ring.middleware params file]
        net.cgrand.moustache
        [clojure.contrib.seq-utils :only [rand-elt]]))

;; Database
(def idk-db (db-init (str (System/getProperty "user.home") "/.idk.db") 30))

(defn integer [s]
   "returns nil if s does not represent an integer"
    (try 
      (Integer/parseInt s)
      (catch Exception e)))

(def head-html
     [:head
      (include-css "/resources/public/css/idk.css")
      (javascript-tag
       "function changeAnswerButton() {
            var txtbox = document.getElementById('anstxtbox');
            var ansb = document.getElementById('ansbutton');
            if (txtbox.value == '')
                ansb.value = 'SKIP'
            else
                ansb.value = 'ANSWER'
       }")
       [:title "I don't know"]])

(def header-html
     [:div#header
      [:h1#title (link-to "/" "I DON'T KNOW")]
      [:h1.header (link-to "/ask" "ASK")]
      [:h1.header (link-to "/answer" "ANSWER")]
      [:h1.header (link-to "/archive" "ARCHIVE")]])

(defn unanswered []
  (let [ids (keys (:data @idk-db))]
    (filter (fn [x] (and (not (:answer (db-get idk-db x))) (not= x :next-id))) ids)))

(defn home-html []
     (html
      (:html4 doctype)
      head-html
      [:body
       header-html
       [:div#body
        [:table {:border "0" :width "100%"}
         [:tr
          [:td {:align "center"} [:h1.huge (link-to "/ask" "ASK")]]
          [:td {:align "center"} [:h1.huge (link-to "/answer" "ANSWER")]]]]
        [:table {:border "0" :width "100%"}
         (map (fn [x] [:tr [:h1 (link-to (str "/answer/" x) (:question (db-get idk-db x)))]]) (unanswered))]
        [:h1 (link-to "/archive" "ARCHIVE")]]]))

(def ask-html
     (html
      (:html4 doctype)
      head-html
      [:body
       header-html
       [:div#body
        [:h1.huge "ASK"]
        [:form {:method "POST" :action "/ask"}
         [:input.ask {:type "text" :name "question" :size "80"}]
         [:br][:br]
         [:input.ask {:type "submit" :value "ASK"}]]]]))

(defn question-html [id]
  (let [{question :question answer :answer} (db-get idk-db id)]
    (html
     (:html4 doctype)
     head-html
     [:body
      header-html
      (if question
        [:div#body
         [:h1.huge "QUESTION:"]
         [:h1 question]
         [:h1.huge "ANSWER:"]
         (if answer
           [:div
            [:h1 answer]
            [:form {:method "POST" :action "/ask"}
             [:input {:type "hidden" :name "question" :value question}]
             [:input {:type "hidden" :name "id" :value (str id)}]
             [:input.ask {:type "submit" :value "ASK AGAIN"}]]]
           [:h1 (link-to (str "/answer/" id) "This question has not been answered yet")])]
        [:div#body
         [:h1.huge "NO SUCH QUESTION"]])])))

(defn answer-html [id]
  (let [{question :question answer :answer} (db-get idk-db id)]
    (html
     (:html4 doctype)
     head-html
     [:body {:onload "document.getElementById('ansbutton').value = 'SKIP';"}
      header-html
      (if question
        [:div#body
         [:h1.huge "QUESTION:"]
         [:h1 question]
         [:h1.huge "ANSWER:"]
         [:form {:method "POST" :action "/answer"}
          [:input {:type "hidden" :name "id" :value (str id)}]
          [:input#anstxtbox {:class "ask" :type "text" :name "answer" :size "80" :onkeyup "changeAnswerButton();"}]
          [:br][:br]
          [:input#ansbutton {:class "ask" :type "submit" :value "ANSWER"}]]]
        [:div#body
         [:h1.huge "NO SUCH QUESTION"]])])))

(defn archive-html []
  (let [ids (keys (:data @idk-db))]
    (html
     (:html4 doctype)
     head-html
     [:body
      header-html
      [:div#body
       [:h1.huge "ARCHIVE"]
       [:table {:border "0" :width "100%"}
        (map (fn [x] [:tr [:h1 (link-to (str "/" x) (:question (db-get idk-db x)))]]) ids)]]])))

(defn home-handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (home-html)})

(defn ask-get-handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body ask-html})

(defn ask-post-handler [{params :params :as request}]
  (if (and (params "question") (not= (count (params "question")) 0))
    (let [question (if (= (last (params "question")) \?) (params "question") (str (params "question") "?"))
          id (if (params "id") (integer (params "id")) (db-get idk-db :next-id 1))]
      (db-assoc idk-db id {:question question :answer nil})
      (db-assoc idk-db :next-id (inc (db-get idk-db :next-id 1)))
      {:status 307
       :headers {"Location" (str "/" id)}})
    (ask-get-handler request)))

(defn answer-id-handler [request id]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (answer-html id)})

(defn answer-get-handler [request]
  (if (seq (unanswered))
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (answer-html (rand-elt (unanswered)))}
    {:status 307
     :headers {"Location" "/"}}))


(defn answer-post-handler [{params :params :as request}]
  (if (and (params "id") (params "answer") (not= (count (params "answer")) 0))
    (let [id (integer (params "id"))
          answer (params "answer")]
      (db-assoc idk-db id (assoc (db-get idk-db id) :answer answer))
      (answer-get-handler request))
    (answer-get-handler request)))

(defn question-handler [request id]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (question-html id)})

(defn archive-handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (archive-html)})

(def routes
     (app
      (wrap-params)
      (wrap-file (System/getProperty "user.dir"))
      [""] home-handler
      ["ask"] {:get ask-get-handler :post ask-post-handler}
      ["answer" [id integer]] (fn [req] (answer-id-handler req id))
      ["answer"] {:get answer-get-handler :post answer-post-handler}
      ["archive"] archive-handler
      [[id integer]] (fn [req] (question-handler req id))))

(defn idk [] (run-jetty #'routes {:port 8080}))
