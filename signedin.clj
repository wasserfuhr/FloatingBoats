(fn[request response]
 (let[
    AppVars(.getAttribute request "vars")
    service(:datastoreService AppVars)
    apiKey"7fa86b8e46a5894fa24b58da34f413723531c0ee"
    token(.getParameter request "token")
    go(.getParameter request "go")
    ;http://developers.janrain.com/documentation/api/auth_info/
    urlS (str
     "https://rpxnow.com/api/v2/auth_info?"
     "apiKey=" apiKey
     "&token=" token)
    rpxResponse(let[url(java.net.URL. urlS)]
        (with-open[stream (. url (openStream))]
          (let [buf (java.io.BufferedReader. (java.io.InputStreamReader. stream))]
            ((defn a [b]
              (let [line (.readLine b)]
                (if (nil? line) ""
                 (str line (a b)))))
             buf))))
    json(clojure.contrib.json/read-json rpxResponse)
    sessId(.getId (.getSession request))
    sessText (com.google.appengine.api.datastore.Text. rpxResponse)
    e
     (doto
      (com.google.appengine.api.datastore.Entity. "SignIn" sessId)
      (.setProperty "date" (java.util.Date.))
      (.setProperty "content" sessText)
      ;needed for SignIn queries:
      (.setProperty "identifier" (:identifier (:profile json)))
      ;needed for stats queries:
      (.setProperty "providerName" (:providerName (:profile json))))
     user (:displayName (:profile json))
     msgBody
       (str "New " (:appName AppVars) " SignIn by " user
        " via " (:providerName (:profile json)))]
   (do
    ((:notify AppVars)
      (str "A new SignIn by " user " at https://" 
       (:appId AppVars) ".appspot.com/ - 
Identifier: " (:identifier (:profile json)))
       msgBody)
    (.put service e)
    (.sendRedirect response (str (if go go "/") "?signedin"))))))
