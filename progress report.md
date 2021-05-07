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


commit tag <app state saving> in main branch
  Summary
    앱을 껐다 켰을 때 정보를 잃지 않도록 하기 위해 SharedPreferences Object를 이용하여 만들어진 savingStates들을 관리하였고, 
    이 중 현재 active 상태인 profile의 번호를 기억하도록 하였음
    회전 시 앱의 종료 및 재시작의 경우 content main과 listview_item에서 회전 자체를 막아서 방지함
    
    
commit tag <basic activity> in main branch
  Summary
    Main Activity에서 active 상태인 profile이 변경될 때 마다 현재 상태를 savingStates의 설정에 따라 저장/복구
    저장해야할 양이 많아질 수 있기에 SharedPreference가 아닌 앱 내부 저장소에 파일로 쓰고 읽음
    현재는 volume만 조절 가능

commit tag <background location tracking> in main branch
  New class
    LocationTracking - broadcast receiver를 extend한 클래스, Google Api인 Geofence의 pending intent를 받아 특정 영역의 출입 확인 및 그에 따른 state 조작
    NotificationHelper - LocationTracking에서 사용하는 클래스, 특정 영역의 출입이 확인될 시 이를 호출하여 Notification을 띄움
  Summary
    Google Map과 함께 사용할 수 있는 Api인 Geofence를 이용하여 Background에서 위치 추적
    Geofence의 radius는 최소한의 정확도 보장을 위해 150m로 고정하였으며 savingStates에 Geofence의 위도와 경도, 이전 Geofence의 존재 유무에 대한 변수 추가
    SettingsActivity에 GoogleMap으로 Geofence를 설정하는 부분이 추가되었으며 MainActivity에서 이를 읽어 Geofence를 관리하게 됨
    LocationTracking은 받은 Geofence의 Intent에 따라 mainActivity의 instance를 조작하여 State들을 조작
    실제 폰에서도 3~6분 사이의 빈도로 위치 추적 가능
    https://developer.android.com/training/location/geofencing?hl=ko
