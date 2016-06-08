(fn[rq rs](let[v(.getAttribute rq "vars")
tags (.getEnumConstants difflib.DiffRow$Tag)
INSERT (get tags 0)
DELETE (get tags 1)
CHANGE (get tags 2)
EQUAL (get tags 3)
service (:datastoreService v)
   p0 (Long.(first(.getParameterValues rq "revision")))
   p1 (Long.(second(.getParameterValues rq "revision")))
   p0k(com.google.appengine.api.datastore.KeyFactory/createKey"BootStrap"p0)
   p1k(com.google.appengine.api.datastore.KeyFactory/createKey"BootStrap"p1)
   p0e(.get service p0k)
   p1e(.get service p1k)
   p0t(.getTime (.getProperty p0e "date"))
   p1t(.getTime (.getProperty p1e "date"))
   p0s(.getValue (.getProperty p0e "content"))
   p1s(.getValue (.getProperty p1e "content"))
   p0v(java.util.Arrays/asList(.split p0s "\r\n"))
   p1v(java.util.Arrays/asList(.split p1s "\r\n"))
   original(if(< p0t p1t)p0 p1)
   revised(if(< p0t p1t)p1 p0)
   originalV(if(< p0t p1t)p0v p1v)
   revisedV(if(< p0t p1t)p1v p0v)
   generator(.build(new difflib.DiffRowGenerator$Builder))]
  (hiccup.core/html "<!DOCTYPE html>" [:html
   [:head
    [:title "diff « α"]
    [:meta {:http-equiv "Content-Type" :content "text/html;charset=utf-8"}]
    [:link {:rel "stylesheet" :type "text/css" :href "/css"}]
    [:style "pre{padding: 0px; margin: 0px; font-size: 8pt}"]]
   [:body
    [:div
     [:h1[:a{:href"/"}"α"]" » diff"]
     [:table[:tr 
       [:th "#"] [:th "+/-"]
       [:th "old (" [:a {:href (str "/edit/" original)} original] ")"]
       [:th "#"]
       [:th "new (" [:a {:href (str "/edit/" revised)} revised] ")"]]
     ((defn rec [x cl cr](let [f (first x)]
       (cons
        [:tr(if(not= EQUAL(.getTag f)){:style "background-color:#ccc"})
         [:td cl]
          [:td(.getTag (first x))]
          [:td[:pre (.getOldLine (first x))]]
          [:td cr]
          [:td[:pre (.getNewLine (first x))]]]
        (if(first(rest x))
         (rec(rest x)
          (+ cl 1)
          (+ cr(if(= DELETE (.getTag (first x))) 0 1)))))))
      (.generateDiffRows generator originalV revisedV) 1 1)]]]])))
