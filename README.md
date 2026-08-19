<div align="center">

# Random Tour

### 목적지를 숨기면, 평범한 산책이 탐험이 된다

현재 위치 주변의 장소를 무작위로 뽑고<br />
거리·방향·온도 힌트만으로 찾아가는 Android 위치 기반 탐험 앱

![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android-12%2B-3DDC84?logo=android&logoColor=white)
![NAVER Maps](https://img.shields.io/badge/NAVER_Map_SDK-3.23.3-03C75A)
![OpenStreetMap](https://img.shields.io/badge/OpenStreetMap-Overpass_API-7EBC6F?logo=openstreetmap&logoColor=white)

</div>

## 프로젝트 소개

일반적인 지도 앱은 목적지와 최단 경로를 먼저 보여줍니다. Random Tour는 그 정보를 의도적으로 감춰, 익숙한 동네를 게임처럼 탐색하게 만듭니다.

사용자는 반경과 탐험 테마, 힌트 난이도를 정한 뒤 무작위 목적지를 뽑습니다. 탐험 중에는 목적지 이름·핀·경로 대신 남은 거리와 방향, 가까워질수록 변하는 온도 힌트만 확인할 수 있습니다. 도착이 확정되면 장소가 공개되고 탐험 기록이 기기에 저장됩니다.

별도 후보 서버 없이 OpenStreetMap Overpass와 NAVER 지도 심벌을 조합해 실제 주변 장소를 동적으로 수집합니다.

| 구분 | 내용 |
| --- | --- |
| 개발 형태 | 개인 프로젝트 |
| 플랫폼 | Android 12 이상 |
| 구현 범위 | 기획 분석, UI/UX, 위치 추적, 지도 연동, 동적 후보 검색, 로컬 저장, 단위 테스트 |
| 현재 상태 | 서버리스 하이브리드 장소 검색을 포함한 Android MVP |

## 핵심 사용자 흐름

~~~mermaid
flowchart LR
    A[반경·테마·난이도 설정] --> B[현재 위치 확인]
    B --> C[OpenStreetMap 후보 조회]
    C --> D{후보가 충분한가?}
    D -->|아니요| E[NAVER 지도 렌더링]
    E --> F[화면 내 장소 심벌 수집]
    D -->|예| G[거리·이력·중복 검증]
    F --> G
    G --> H[목적지 무작위 추첨]
    H --> I[거리·방향·온도 힌트 탐험]
    I --> J{도착 조건 충족}
    J -->|아니요| I
    J -->|예| K[목적지 공개·기록 저장]
~~~

## 실행 화면

SM-S931N 실기기에서 촬영한 화면입니다.

<table>
  <tr>
    <td align="center" width="50%">
      <img src="docs/screenshots/home.png" alt="Random Tour 홈 화면" width="270" />
      <br />
      <b>홈</b>
      <br />
      탐험 시작과 누적 기록을 한 화면에서 확인
    </td>
    <td align="center" width="50%">
      <img src="docs/screenshots/exploration-setup.png" alt="탐험 설정 화면" width="270" />
      <br />
      <b>탐험 설정</b>
      <br />
      반경과 테마, 힌트 난이도를 조합
    </td>
  </tr>
  <tr>
    <td align="center" width="50%">
      <img src="docs/screenshots/location-permission.png" alt="위치 권한 요청 화면" width="270" />
      <br />
      <b>위치 권한</b>
      <br />
      탐험을 시작할 때만 정확한 위치 권한 요청
    </td>
    <td align="center" width="50%">
      <img src="docs/screenshots/destination-ready.png" alt="목적지 추첨 성공 화면" width="270" />
      <br />
      <b>목적지 추첨</b>
      <br />
      후보 수와 거리만 공개하고 장소 정보는 잠금
    </td>
  </tr>
  <tr>
    <td align="center" colspan="2">
      <img src="docs/screenshots/candidate-error.png" alt="목적지 후보 검색 실패 화면" width="270" />
      <br />
      <b>검색 실패 복구</b>
      <br />
      재시도하거나 반경을 넓혀 다음 행동으로 연결
    </td>
  </tr>
</table>

## 주요 기능

| 영역 | 구현 내용 |
| --- | --- |
| 탐험 설정 | 500m·1km·2km 반경, 완전 랜덤·카페·먹거리·산책·문화 테마, 3단계 힌트 난이도 |
| 동적 장소 검색 | OpenStreetMap 태그 기반 반경 검색과 NAVER 지도 심벌 폴백을 서버 없이 실행 |
| 목적지 추첨 | 최소 거리·선택 반경·최근 방문·제외 카테고리·중복 후보 검증 후 무작위 선택 |
| 비밀 탐험 | 도착 전 목적지 이름, 지도 핀, 이동 경로를 숨기고 거리·방향·온도만 제공 |
| 위치 추적 | GPS 위치와 정확도를 반영해 남은 거리, 방위각, 이동 거리를 실시간 갱신 |
| 도착 판정 | 50m 이내이면서 위치 정확도 35m 이하인 상태가 3회 연속 확인될 때 도착 확정 |
| 기록과 설정 | 완료한 탐험 최대 100건, 기본 반경·테마·난이도·제외 카테고리를 기기에 저장 |
| 예외 처리 | 권한 거부, GPS 약함, 공개 API 지연, 후보 없음에 맞는 안내와 재시도 동선 제공 |

## 아키텍처

~~~mermaid
flowchart LR
    UI[Compose UI] --> VM[RandomTourViewModel<br/>StateFlow]
    UI <--> MAP[NAVER Map SDK]
    MAP -. pickAll Symbol .-> UI
    VM --> CANDIDATE[CandidateRepository]
    VM --> LOCATION[LocationRepository]
    VM --> STORE[ExplorationStore]
    CANDIDATE --> OVERPASS[OpenStreetMap<br/>Overpass API]
    CANDIDATE --> GEOCODER[Android Geocoder<br/>역지오코딩만 사용]
    LOCATION --> LM[Android LocationManager]
    STORE --> PREF[SharedPreferences + JSON]
~~~

화면은 단일 UI 상태를 구독하고 사용자 이벤트만 ViewModel에 전달합니다. 장소 검색, 위치 추적, 로컬 저장은 각각 분리했으며 거리·방위각·도착 판정과 Overpass 쿼리 생성은 JVM 단위 테스트가 가능한 순수 로직으로 구성했습니다.

## 기술적 의사결정

### 1. 후보 서버 없는 이중 장소 소스

1차 검색은 좌표·반경·테마를 Overpass QL로 변환해 OpenStreetMap의 카페, 음식점, 공원, 문화 공간을 조회합니다. 값 정규식 대신 인덱스를 활용할 수 있는 태그 정확 일치 쿼리와 최대 100건 제한을 적용해 밀집 지역 응답 시간을 줄였습니다.

완전 랜덤 후보가 5곳 미만이거나 선택 테마의 후보가 0곳이면 NAVER 지도를 해당 반경으로 렌더링합니다. 렌더링이 안정된 뒤 `NaverMap.pickAll()`로 화면에 표시된 `Symbol`의 이름과 좌표를 수집하고 기존 후보와 병합합니다. 오래된 재시도 결과는 요청 번호로 무시하며 지도 응답이 오지 않으면 타임아웃 후 안전하게 종료합니다.

이 구조는 NAVER Local Search Client Secret이나 별도 후보 서버 없이 동작합니다. OpenStreetMap 데이터가 선택되면 앱 안에 저작자 및 ODbL 출처를 표시합니다.

### 2. 모든 후보를 클라이언트에서 다시 검증

~~~text
좌표 유효성 검사
→ Haversine 거리 재계산
→ 120m 이상, 선택 반경 이내만 유지
→ 최근 30일 방문 장소와 제외 카테고리 제거
→ 이름 + 좌표 기준 중복 제거
→ 남은 후보 중 무작위 선택
~~~

검색 공급자가 달라져도 탐험 규칙은 Android 클라이언트에서 동일하게 유지됩니다. NAVER 지도 심벌은 카테고리를 직접 제공하지 않으므로 테마 키워드와 대표 브랜드를 먼저 매칭하고, 일치 결과가 없을 때만 일반 장소를 사용합니다.

### 3. GPS 오차를 고려한 연속 도착 판정

한 번의 좌표만으로 도착 처리하면 GPS 튐으로 잘못 완료될 수 있습니다. 거리와 정확도 조건을 모두 만족한 샘플이 3회 연속 들어왔을 때만 도착을 확정하고, 부정확한 샘플이 들어오면 누적 횟수를 초기화합니다.

### 4. 목적지 비공개를 상태 규칙으로 관리

도착 전에는 지도에 목적지 마커와 경로를 전달하지 않습니다. 장소명 공개 여부도 탐험 상태로 제어해 화면 변경 과정에서 목적지가 우연히 노출되는 가능성을 줄였습니다.

### 5. 위치 사용 범위와 데이터 출처 공개

백그라운드 위치 권한은 요청하지 않습니다. 후보 검색 시 현재 좌표와 반경이 공개 Overpass 서비스에 전달된다는 점을 설정 화면에 안내하고, OpenStreetMap 후보에는 클릭 가능한 출처 표시를 제공합니다.

## 기술 스택

| 분류 | 기술 |
| --- | --- |
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose, Material 3 |
| State | ViewModel, StateFlow |
| Map | NAVER Map SDK 3.23.3 |
| Place Data | OpenStreetMap Overpass API, NAVER Map Symbol |
| Location | Android LocationManager, Geocoder |
| Network | HttpURLConnection, JSON |
| Local Data | SharedPreferences, JSON |
| Test | JUnit 4 |
| Build | Android Gradle Plugin 9.2.1, Gradle Kotlin DSL |

외부 라이브러리를 필요한 범위로 제한해 MVP의 데이터 흐름과 예외 처리를 코드에서 바로 추적할 수 있도록 했습니다.

## 프로젝트 구조

~~~text
app/src/main/java/com/chlqudco/randomtour/
├── MainActivity.kt
├── RandomTourApp.kt
├── RandomTourViewModel.kt
├── Models.kt
├── ExplorationMath.kt
├── CandidateRepository.kt
├── LocationRepository.kt
├── ExplorationStore.kt
├── NaverExplorationMap.kt
└── ui/theme/

app/src/test/java/com/chlqudco/randomtour/
├── ExplorationMathTest.kt
└── OverpassQueryBuilderTest.kt
~~~

## 실행 방법

### 1. 환경 준비

- Android Studio와 내장 JBR
- Android 12(API 31) 이상 기기 또는 에뮬레이터
- NAVER Cloud Platform Maps에 등록된 Android 앱의 Dynamic Map Client ID
- 위치와 인터넷 연결

### 2. NAVER 지도 Client ID 설정

프로젝트 루트의 `local.properties`에 다음 값을 추가합니다.

~~~properties
NAVER_MAP_API_KEY=발급받은_Dynamic_Map_Client_ID
~~~

NAVER Cloud Platform에 등록한 Android 패키지 이름은 `com.chlqudco.randomtour`와 일치해야 합니다. `local.properties`는 Git에 포함하지 않습니다.

OpenStreetMap Overpass는 별도 키가 필요하지 않으며 후보 서버 주소도 설정하지 않습니다.

### 3. 빌드 및 테스트

~~~powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
~~~

생성된 APK는 `app/build/outputs/apk/debug/app-debug.apk`에서 확인할 수 있습니다.

## 검증 결과

| 검증 | 결과 |
| --- | --- |
| JVM 단위 테스트 | 거리·방향·도착 판정·Overpass 쿼리 테스트 포함 전체 8건 통과 |
| Debug 빌드 | `assembleDebug` 성공 |
| Android Lint | 오류 0건 |
| 실기기 확인 | SM-S931N에서 2km OpenStreetMap 후보 86곳 추첨과 NAVER 지도 폴백 후보 37곳 수집 확인 |

후보 수는 현재 위치, 탐험 테마, 지도 렌더링 시점, OpenStreetMap 데이터 상태에 따라 달라집니다.

## 한계와 다음 단계

- 공개 Overpass 인스턴스의 혼잡이나 데이터 밀도에 따라 검색 결과와 시간이 달라질 수 있음
- NAVER 지도 심벌은 카테고리를 제공하지 않아 폴백 테마 분류가 이름 기반 휴리스틱에 의존함
- 탐험 진행 상태의 프로세스 종료 복원
- DataStore 또는 Room 기반 저장 계층 확장
- Compose UI 자동화 테스트와 장시간 야외 위치 추적 테스트
- 대규모 서비스 전환 시 Overpass 캐시 또는 자체 인스턴스 검토

## 데이터 출처와 문서

- [NAVER Map Android SDK](https://navermaps.github.io/android-map-sdk/guide-ko/)
- [NAVER Map Symbol API](https://navermaps.github.io/android-map-sdk/reference/com/naver/maps/map/Symbol.html)
- [OpenStreetMap Overpass API](https://wiki.openstreetmap.org/wiki/Overpass_API)
- [OpenStreetMap Copyright and License](https://www.openstreetmap.org/copyright)
