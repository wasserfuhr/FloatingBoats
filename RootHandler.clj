(fn[rq rs](let[log(java.util.logging.Logger/getLogger"mind.RootHandler")
 apps{
  :dresdenlabs{:name"AlphaLabs"}
  :new-egypt{:name"NewEgypt"}}
 appId(.get com.google.appengine.api.utils.SystemProperty/applicationId)
 app(get apps(keyword appId))
 pageEdit
  (fn[href title size]
   [:sup[:a{:href(str"/edit?filter="href):title title}
    [:img{:width size :height size :alt title
      :src"https://ssl.gstatic.com/codesite/ph/images/pencil-y14.png"}]]])
 userService(com.google.appengine.api.users.UserServiceFactory/getUserService)
 currUser(.getCurrentUser userService)
 datastoreService (com.google.appengine.api.datastore.DatastoreServiceFactory/getDatastoreService)
 sortDirAsc(com.google.appengine.api.datastore.Query$SortDirection/valueOf "ASCENDING")
 sortDirDesc(com.google.appengine.api.datastore.Query$SortDirection/valueOf "DESCENDING")
 filterEq(com.google.appengine.api.datastore.Query$FilterOperator/valueOf "EQUAL")
 queryLimit(fn[n]
  (com.google.appengine.api.datastore.FetchOptions$Builder/withLimit n))
 memCache(com.google.appengine.api.memcache.MemcacheServiceFactory/getMemcacheService)
 p(java.util.regex.Pattern/compile "[A-Z]+[a-z]+[A-Z]+[a-zA-Z0-9]*")
 urlP(java.util.regex.Pattern/compile "http[s]?://(^[ \n\r]*)");ToDo
 linkify(fn[wikiString]
  (let[m(.matcher urlP wikiString)]
   (if(.find m)
    (let[s(subs wikiString(.start m)(.end m))]
     [:a{:href(str"http://"s)}s])
    wikiString)))
 wikify
    (fn[wikiString]
      (let[urlM(.matcher urlP wikiString)]
      ((fn recurW[wikiString m lastPos]
        (if(.find m)
         (let[
           matchS(subs wikiString(.start m)(.end m))
           pageExists
            (.getResourceAsStream(RT/baseLoader)
             (str "mind/" matchS ".txt"))]
          (list
           (subs wikiString lastPos(.start m))
           [:a {
             :href
              (str"/wiki/"(linkify matchS))
             :class(if pageExists"blue""red")}             
            (linkify matchS)]
           (recurW wikiString m(.end m))))
         (subs(str wikiString" ")lastPos)))
        wikiString
        (.matcher p wikiString)0)))
 btnBase"/"
 imgBase"/"
 knownUsers{
    ; key: the userId as returned by userService.getCurrentUser().getUserId()
    "105367551153881701299"{
     :profilePic "https://graph.facebook.com/509034915/picture"
     :wikiName"RaWa"}
    "ekcup" {
     :profilePic "https://lh3.googleusercontent.com/-1mLgZCinFfY/AAAAAAAAAAI/AAAAAAAAABE/R6k8EnyaYTg/s120-c/photo.jpg"
     :wikiName "HiPo"}}
 statsKey(com.google.appengine.api.datastore.KeyFactory/createKey "__Stat_Kind__" "BootStrap")
 countPages(fn[](.getProperty(.get datastoreService statsKey)"count"))
 uri(str" "(.getRequestURI rq))
 uriP(if(<= (.length uri)2)"/"(.trim uri))
 pathElements(.split uriP"/")
 resource(if(> (alength pathElements)0)(second pathElements)"")
 service (com.google.appengine.api.datastore.DatastoreServiceFactory/getDatastoreService)
 printException
    (fn[e]
     (hiccup.core/html "<!DOCTYPE html>"
      [:html
       [:head
        [:title"Exception"]
        [:link{:rel"stylesheet":type"text/css":href"/css"}]]
       [:body
        [:div
         [:h1"Exception"]
         [:p"Pieschen we have a ProBlem:"[:br]e]]]]))
 evalCode(fn[code]
  (try
   (.setCharacterEncoding rs"UTF-8")
   ((eval(read-string code))rq rs)
   (catch Exception e
    (do
     (.severe log(str"ErrorMessage:"e))
     (printException e)))))
 evalId(fn[id]
  (let[l(Long. id)
    key(com.google.appengine.api.datastore.KeyFactory/createKey"BootStrap"l)]
   (evalCode(.getValue(.getProperty(.get service key)"content")))))
 evalResource(fn[r]
  (let[
    limit(com.google.appengine.api.datastore.FetchOptions$Builder/withLimit 1) 
    query(doto
     (com.google.appengine.api.datastore.Query."BootStrap")
     (.addSort"published"sortDirDesc)
     (.addFilter"resource"filterEq(str"/"r)))
    codeE(first(.asIterable(.prepare service query)limit))]
   (evalCode
    (if codeE(.getValue(.getProperty codeE"content"))
     (slurp(str"https://raw.githubusercontent.com/wasserfuhr/"
      "AllHashes/master/r/"r".clj"))))))
 rpxJs[:script {:type "text/javascript"}
"  var rpxJsHost=(('https:'==document.location.protocol) ? 'https://' : 'http://static.');
  document.write(unescape(\"%3Cscript src='\"+rpxJsHost+
\"rpxnow.com/js/lib/rpx.js' type='text/javascript'%3E%3C/script%3E\"));
  RPXNOW.overlay=true;
  RPXNOW.language_preference='en';"]
 search
  [:span
   [:input {:style "margin-left: 64px" :name "q"}]
   [:input {:type "submit" :value "MindSearch"}]]
 auth(fn[]
     (let[
       sessId(.getId (.getSession rq))
       sessKey(com.google.appengine.api.datastore.KeyFactory/createKey "SignIn" sessId)
       sessE
        (try
         (.get service sessKey)
         (catch com.google.appengine.api.datastore.EntityNotFoundException e nil))
       json
        (if sessE
         (clojure.contrib.json/read-json
          (.getValue(.getProperty sessE "content"))))]
      [:form{:action"/search"}
       (if json;(.isUserLoggedIn userService)
        [:nav
         [:p#auth
          "WelCome "
          [:a{:href(:identifier(:profile json))}
           (:displayName(:profile json))" "
           [:img{:width 25 :height 25 :src(:photo(:profile json))}]]
          " | "
          [:a{:href"/signout"}"SignOut"]]
         [:p[:a{:href"/edit"}"WebEditor"] search]]
        [:nav
         [:p#auth
          " WelcomeVisitor | SignIn: "
          [:a{:class"rpxnow":onclick"return false;"
             :href(str
              "https://pieschenbank.rpxnow.com/openid/v2/signin?"
              "token_url=https%3A%2F%2F" appId ".appspot.com%2Fsignedin"
              "%3Fgo="(.getRequestURI rq))}
            (map
             (fn[s]
              [:span[:img{:src(str imgBase s"SignInLogo.png")}]" "])
             [])
            "more..."]]
         [:p[:a{:href "/edit"}"WebEditor"]search]])]))
 footer[:footer(wikify"a RaWa EtAl production.")]
 notifyTo(fn[to subject msgBody]
   (let[
     props(java.util.Properties.)
     session(javax.mail.Session/getDefaultInstance props nil)
     fromAddr
      (javax.mail.internet.InternetAddress.
       "rainerwasserfuhr@googlemail.com""RainerWasserfuhr")
     to"wasserfuhr@yahoo.com"
     toAddr
      (javax.mail.internet.InternetAddress.
       to"RainerWasserfuhr")
     msg
      (doto(javax.mail.internet.MimeMessage. session)
       (.setFrom fromAddr)
       (.addRecipient
          javax.mail.Message$RecipientType/TO toAddr)
       (.setSubject subject)
       (.setText msgBody))]
    (javax.mail.Transport/send msg)))
 notify(fn[subj msg](notifyTo"wasserfuhr@yahoo.com"subj msg))
 id(if resource(get pathElements 2))]
(do;utils made available for each ResourceHandler:
 (.setAttribute rq"requestURI"
  (str resource
    (.trim
     (subs(str(.getRequestURI rq)"      ")
      (.length(str"/eval/"id))))))
 (.setAttribute rq"c"{
    :sh(fn[h](slurp(str"http://s.sl4.eu/c/sh/"h)))
    :h(fn[h](slurp(str"http://s.sl4.eu/c/h/"h)))
    :re(fn[s](.sendRedirect rs s))
    :ht(fn[h b](hiccup.core/html"<!DOCTYPE html>"[:html h b]))
    :css[:link{:rel"stylesheet":type"text/css":href"/css"}]
    :vp[:meta{:name"viewport":content"width=device-width,initial-scale=1.0"}]
    :can[:link{:rel"canonical":href(str"http://sl4.eu"(.getRequestURI rq))}]})
 (.setAttribute rq"vars"{
    :analytics ""
    :auth auth
    :rpxJs rpxJs
    :log log
    :sortDirAsc sortDirAsc
    :sortDirDesc sortDirDesc
    :filterEq filterEq
    :queryLimit queryLimit
    :appId appId
    :evalResource evalResource
    :pageEdit pageEdit
    :appName(:name app)
    :notify notify
    :notifyTo notifyTo
    :apps apps
    :countPages countPages
    :datastoreService datastoreService
    :footer footer
    :knownUsers knownUsers
    :memCache memCache
    :userService userService
    :wikify wikify})
 (if(= resource"eval")(evalId id)(evalResource resource)))))
