(fn [request response]
 (let [
   log (java.util.logging.Logger/getLogger "mind.RootHandler")
   ;each known BigTableNomic WebApp:
   apps {
    :bahnchefspiel
     {:appStats true
      :name "BahnChefSpiel"}
    :bigtablenomic
     {:name "BigTableNomic"
      :appStats true
      :analyticsProfileId 37874819
      :analyticsToken 38284150
      :analyticsPropertyId "UA-5959916-3"}
    :corisotto
     {:name "CoRisotto"}
    :dresdenbot
     {:appStats true
      :name "DresdenBot"}
    :elias-morgenstern
     {:name "EliasMorgenstern"}
    :etherbank
     {:name "EtherBank"}
    :flavourcity
     {:name "FlavourCity"
      :appStats true
      :analyticsProfileId 61681360
      :analyticsToken 60310363
      :analyticsPropertyId "UA-5959916-10"}
    :heidi-morgenstern
     {:name "HeidiMorgenstern"
      :appStats true
      :analyticsProfileId 49416747
      :analyticsPropertyId "UA-5959916-8"}
    :hotelkronentor
     {:name "HotelKronentor"}
    :myviaf
     {:name "MyViaf"}
    :new-egypt
     {:name "NewEgypt"
      :appStats true
      :analyticsProfileId 42011666
      :analyticsToken 42107041
      :analyticsPropertyId "UA-5959916-6"
      :highReplication true}
    :noobootstrap
     {:name "NooBootStrap"}
    :noopolis 
     {:name "NooPolis"
      :appStats true
      :analyticsProfileId 42225147
      :analyticsToken 42300499
      :analyticsPropertyId "UA-5959916-7"}
    :pieschenai
     {:name "PieschenAi"}
    :pieschenartgroup
     {:name "PieschenArtGroup"}
    :pieschenbank
     {:name "PieschenBank"
      :appStats true
      :analyticsProfileId 37873294
      :analyticsToken 38285507
      :analyticsPropertyId "UA-5959916-4"}
    :singularacademy
     {:name "SingularAcademy"
      :appStats true
      :analyticsProfileId 50096408
      :analyticsToken 49573415
      :analyticsPropertyId "UA-5959916-9"}
    :singulartheater
     {:name "SingularTheater"}}
   appId
    ;special case: a "HighReplication" WebApp appId starts with "s~"
    ;which we have to remove: 
    (.replaceAll
     (.get com.google.appengine.api.utils.SystemProperty/applicationId)
     "s~" "")
   app (get apps (keyword appId))
   appName (:name app)
   pageEdit
     (fn [href title size]
      [:sup
       [:a
        {:href (str "/edit?filter=" href)
         :title title :alt title}
         [:img {:width size :height size
          :src "https://ssl.gstatic.com/codesite/ph/images/pencil-y14.png"}]]])
   userService (com.google.appengine.api.users.UserServiceFactory/getUserService)
   currUser (.getCurrentUser userService)
   datastoreService (com.google.appengine.api.datastore.DatastoreServiceFactory/getDatastoreService)
   sortDirAsc (com.google.appengine.api.datastore.Query$SortDirection/valueOf "ASCENDING")
   sortDirDesc (com.google.appengine.api.datastore.Query$SortDirection/valueOf "DESCENDING")
   filterEq (com.google.appengine.api.datastore.Query$FilterOperator/valueOf "EQUAL")
   queryLimit
    (fn [n]
     (com.google.appengine.api.datastore.FetchOptions$Builder/withLimit n))
   memCache (com.google.appengine.api.memcache.MemcacheServiceFactory/getMemcacheService)
   analytics
    [:script {:type "text/javascript"}
"  var _gaq = _gaq || [];
  _gaq.push(['_setAccount', '" (:analyticsPropertyId app) "']);
  _gaq.push(['_trackPageview']);
  (function() {
    var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
    ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
  })();"]
   p (java.util.regex.Pattern/compile
     "[A-Z]+[a-z]+[A-Z]+[a-zA-Z0-9]*")
   urlP (java.util.regex.Pattern/compile
     "http[s]?://(^[ \n\r]*)") ; ToDo
   linkify
    (fn [wikiString]
     (let [
       m (.matcher urlP wikiString)]
      (if (.find m)
       (let [
         s (.substring wikiString (.start m) (.end m))]
        [:a {:href (str "http://" s)}
           s])
       wikiString)))
   wikify
    (fn [wikiString]
      (let [
        urlM (.matcher urlP wikiString)]
      ((fn recurW [wikiString m lastPos]
        (if (.find m)
         (let [
           matchS (.substring wikiString (.start m) (.end m))
           pageExists
            (.getResourceAsStream (RT/baseLoader)
             (str "mind/" matchS ".txt"))]
          (list
           (.substring wikiString lastPos (.start m))
           [:a {
             :href 
              (str
               (if pageExists
                "/wiki/"
                "/wiki/"
                ;"https://code.google.com/p/bigtablenomic/wiki/"
               )
               (linkify matchS))
             :class (if pageExists "blue" "red")}             
            (linkify matchS)]
           (recurW wikiString m (.end m))))
         (.substring (str wikiString " ") lastPos)))
        wikiString
        (.matcher p wikiString)0)))
   btnBase "https://code.google.com/p/bigtablenomic/"
   imgBase "https://bigtablenomic.googlecode.com/files/"
   knownUsers {
    ; key: the userId as returned by userService.getCurrentUser().getUserId()
    "105367551153881701299" {
     :profilePic "https://graph.facebook.com/RainerWasserfuhr/picture"
     :wikiName "RainerWasserfuhr"}}
   statsKey (com.google.appengine.api.datastore.KeyFactory/createKey "__Stat_Kind__" "BootStrap")
   countPages
    (fn [] (.getProperty (.get datastoreService statsKey) "count"))
   uri (str " " (.getRequestURI request))
   uriP (if (<= (.length uri) 2) "/" (.trim uri))
   pathElements (.split uriP "/")
   resource
    (if (> (alength pathElements) 0)
     (second pathElements)
     "")
   ;ToDo:
   service (com.google.appengine.api.datastore.DatastoreServiceFactory/getDatastoreService)
   printException
    (fn [e]
     (hiccup.core/html "<!DOCTYPE html>"
      [:html
       [:head
        [:meta {:http-equiv "Content-type"
                 :content "text/html; charset=utf-8"}]
        [:title "Exception"]
        [:link {:rel "stylesheet" :type "text/css" :href "/css"}]
        (analytics)]
       [:body
        [:div
         [:h1 "Exception"]
         [:p "Pieschen we have a problem:" [:br] e]]]]))
   evalCode
    (fn [code]
     (try
      ((eval (read-string code)) request response)
      (catch Exception e
       (do
        (.severe log (str "An error message:" e))
        (printException e)))))
   evalId
    (fn [id]
     (let [
       key (com.google.appengine.api.datastore.KeyFactory/createKey "BootStrap" (Long. id))]
      (evalCode (.getValue (.getProperty (.get service key) "content")))))
   evalResource
    (fn [resource]
     (let [
       limit (com.google.appengine.api.datastore.FetchOptions$Builder/withLimit 1) 
       query 
        (doto
         (com.google.appengine.api.datastore.Query. "BootStrap")
         (.addSort "published" sortDirDesc)
         (.addFilter "resource" filterEq (str "/" resource)))
       codeE (first (.asIterable (.prepare service query) limit))]
      (evalCode (.getValue (.getProperty codeE "content")))))
   rpxJs
    [:script {:type "text/javascript"}
"  var rpxJsHost = (('https:' == document.location.protocol) ? 'https://' : 'http://static.');
  document.write(unescape(\"%3Cscript src='\" + rpxJsHost +
\"rpxnow.com/js/lib/rpx.js' type='text/javascript'%3E%3C/script%3E\"));
  RPXNOW.overlay = true;
  RPXNOW.language_preference = 'en';"]
   search
    [:span
     [:input {:style "margin-left: 64px" :name "q"}]
     [:input {:type "submit" :value "MindSearch"}]]
   auth
    (fn []
     (let [
       sessId (.getId (.getSession request))
       sessKey (com.google.appengine.api.datastore.KeyFactory/createKey "SignIn" sessId)
       sessE
        (try
         (.get service sessKey)
         (catch com.google.appengine.api.datastore.EntityNotFoundException e nil))
       json
        (if sessE
         (clojure.contrib.json/read-json
          (.getValue (.getProperty sessE "content"))))]
      [:form {:action "/search"}
       (if json ; (.isUserLoggedIn userService)
        [:nav
         [:p#auth
          "WelCome "
          [:a {:href (:identifier (:profile json))}
           (:displayName (:profile json)) " "
           [:img {:width 25 :height 25 :src (:photo (:profile json))}]]
          " | "
          [:a {:href "/signout"} "SignOut"]]
         [:p
          [:a {:href (str btnBase "wiki/DoIt")} "DoIt"] " | "
          [:a {:href (str btnBase "wiki/MindMark")} "MindMark"] " | "
          [:a {:href "/RecentChanges"} "RecentChanges"] " | "
          [:a {:href (str btnBase "wiki/ReDo")} "ReDo"] " | "
          [:a {:href "/edit"} "WebEditor"]
          search]]
        [:nav
         [:p#auth
          " WelcomeVisitor | SignIn: "
          [:a {:class "rpxnow" :onclick "return false;"
             :href (str
              "https://pieschenbank.rpxnow.com/openid/v2/signin?"
              "token_url=https%3A%2F%2F" appId ".appspot.com%2Fsignedin"
              "%3Fgo=" (.getRequestURI request))}
            (map
             (fn [s]
              [:span [:img {:src (str imgBase s "SignInLogo.png")}] " "])
             ["FaceBook" "GoogleAccount" "TwittEr"])
            "more..."]]
         [:p
          (wikify "ImPrint | ")
          [:a {:href "/RecentChanges"} "RecentChanges"] " | "
          [:a {:href "/edit"} "WebEditor"]
          search]])]))
   footer
    [:footer
      [:p
       (wikify "a RainerWasserfuhr EtAl production - AnnoDomino2012. ")
       (wikify "This WebSite is PartOf BigTableNomic.")]
      [:p {:style "text-align:right"}
       [:a {:href (str
         "https://appengine.google.com/dashboard?&app_id=" 
         appId)}
        ; http://code.google.com/appengine/downloads.html#Download_the_Google_App_Engine_Buttons
        [:img
         {:src "https://code.google.com/appengine/images/appengine-noborder-120x30.gif"
          :alt "Powered by GoogleAppEngine"}]]]]
   notify
      (fn [subject msgBody]
       (let [
         props (java.util.Properties.)
         session (javax.mail.Session/getDefaultInstance props nil)
         fromAddr
          (javax.mail.internet.InternetAddress.
           "rainerwasserfuhr@googlemail.com" "RainerWasserfuhr")
         to "rainerwasserfuhr+BtnSignIn@googlemail.com"
         toAddr
          (javax.mail.internet.InternetAddress.
           to "RainerWasserfuhr")
         msg
          (doto (javax.mail.internet.MimeMessage. session)
           (.setFrom fromAddr)
           (.addRecipient
              javax.mail.Message$RecipientType/TO toAddr)
           (.setSubject subject)
           (.setText msgBody))]
        (javax.mail.Transport/send msg)))
   id (if resource (get pathElements 2))]
 (do
   ; these utilities are made available for each ResourceHandler:
  (.setAttribute request "requestURI"
   (str resource
    (.trim
     (.substring (str (.getRequestURI request) "      ")
      (.length (str "/eval/" id))))))
  (.setAttribute request "vars" {
     :analytics analytics
     :auth auth
     :rpxJs rpxJs
     :sortDirAsc sortDirAsc 
     :sortDirDesc sortDirDesc
     :filterEq filterEq 
     :queryLimit queryLimit
     :appId appId
     :pageEdit pageEdit
     :appName appName
     :notify notify
     :apps apps
     :countPages countPages
     :datastoreService datastoreService
     :footer footer
     :knownUsers knownUsers
     :memCache memCache
     :userService userService
     :wikify wikify})
  (if (.equals resource "eval")
   (evalId id)
   (evalResource resource)))))