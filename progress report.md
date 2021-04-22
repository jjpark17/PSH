commit tag <first commit>
  Summary
    main에서 develop branch로 분기


commit tag <saving state intent structure> in develop branch
  New class
    MainActivity - 프로그램의 주 기능 담당, content_main.xml 참조
    SettingsActivity - 각 profile을 생성/변경/제작할 수 있음, settings_activity.xml 참조
    savingStates - profile에 들어갈 저장할 state들에 대한 클래스
  
  Summary
    MainActivity에서 Intent를 통해 savingStates를 SettingActivity로 넘겨주고 이를 조작하여 reply로 변경된 savingState 반환
  
  
commit tag <list view> in develop branch
  New class
    CustomAdapter - listview와 Arraylist<savingStates>를 관리하기 위한 클래스, 뷰, 추가/변경/삭제, 탐색 기능 제공, listview_item.xml 참조
  
  Summary
    Mainactivty에 listview 및 CustomAdapter 의 추가로 SettingsActicity에서 조작된 savingState에 따라 profile을 조작


commit tag <list view> in main branch
  Summary
    앱을 껐다 켰을 때 정보를 잃지 않도록 하기 위해 SharedPreferences Object를 이용하여 만들어진 savingStates들을 관리하였고, 
    이 중 현재 active 상태인 profile의 번호를 기억하도록 하였음
    회전 시 앱의 종료 및 재시작의 경우 content main과 listview_item에서 회전 자체를 막아서 방지함
    
