<div align="center">

# Random Tour

### 목적지를 숨기면, 평범한 산책이 탐험이 된다

현재 위치 주변의 장소를 무작위로 뽑고<br />
거리·방향·온도 힌트만으로 찾아가는 Android 위치 기반 탐험 앱

![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android-12%2B-3DDC84?logo=android&logoColor=white)
![NAVER Maps](https://img.shields.io/badge/NAVER_Map_SDK-3.23.3-03C75A)

</div>

## 프로젝트 소개

일반적인 지도 앱은 목적지와 최단 경로를 먼저 보여줍니다. Random Tour는 그 정보를 의도적으로 감춰, 익숙한 동네를 게임처럼 탐색하게 만듭니다.

사용자는 반경과 탐험 테마, 힌트 난이도를 정한 뒤 무작위 목적지를 뽑습니다. 탐험 중에는 목적지 이름·핀·경로 대신 남은 거리와 방향, 가까워질수록 변하는 온도 힌트만 확인할 수 있습니다. 도착이 확정되면 장소가 공개되고 탐험 기록이 기기에 저장됩니다.

| 구분 | 내용 |
| --- | --- |
| 개발 형태 | 개인 프로젝트 |
| 플랫폼 | Android 12 이상 |
| 구현 범위 | 기획 분석, UI/UX, 위치 추적, 지도 연동, 후보 검증, 로컬 저장, 단위 테스트 |
| 현재 상태 | Android 클라이언트 MVP |

## 핵심 사용자 흐름

~~~mermaid
flowchart LR
    A[반경·테마·난이도 설정] --> B[주변 장소 후보 수집]
    B --> C[거리·중복·방문 이력 검증]
    C --> D[목적지 무작위 추첨]
    D --> E[거리·방향·온도 힌트 탐험]
    E --> F{도착 조건 충족}
    F -->|아니요| E
    F -->|예| G[목적지 공개·기록 저장]
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
| 목적지 추첨 | 현재 위치 기준 후보 수집, 최소 거리·선택 반경·최근 방문·제외 카테고리·중복 후보 검증 |
| 비밀 탐험 | 도착 전 목적지 이름, 지도 핀, 이동 경로를 숨기고 거리·방향·온도만 제공 |
| 위치 추적 | GPS 위치와 정확도를 반영해 남은 거리, 방위각, 이동 거리를 실시간 갱신 |
| 도착 판정 | 50m 이내이면서 위치 정확도 35m 이하인 상태가 3회 연속 확인될 때 도착 확정 |
| 기록과 설정 | 완료한 탐험 최대 100건, 기본 반경·테마·난이도·제외 카테고리를 기기에 저장 |
| 예외 처리 | 권한 거부, GPS 약함, 후보 없음, 네트워크 실패에 맞는 안내와 재시도 동선 제공 |

## 아키텍처

~~~mermaid
flowchart LR
    UI[Compose UI] --> VM[RandomTourViewModel<br/>StateFlow]
    UI --> MAP[NAVER Map SDK]
    VM --> CANDIDATE[CandidateRepository]
    VM --> LOCATION[LocationRepository]
    VM --> STORE[ExplorationStore]
    CANDIDATE --> API[Candidate API]
    CANDIDATE -. 개발용 대체 검색 .-> GEOCODER[Android Geocoder]
    LOCATION --> LM[Android LocationManager]
    STORE --> PREF[SharedPreferences + JSON]
~~~

화면은 단일 UI 상태를 구독하고 사용자 이벤트만 ViewModel에 전달합니다. 장소 검색, 위치 추적, 로컬 저장은 각각의 저장소로 분리해 UI와 Android API 의존성을 나눴습니다. 거리·방위각·도착 판정은 순수 Kotlin 도메인 로직으로 분리해 JVM 단위 테스트가 가능하도록 구성했습니다.

## 기술적 의사결정

### 1. 목적지 후보를 클라이언트에서 다시 검증

후보 API의 결과를 그대로 사용하지 않고 다음 파이프라인을 통과시킵니다.

~~~text
좌표 유효성 검사
→ Haversine 거리 재계산
→ 120m 이상, 선택 반경 이내만 유지
→ 최근 30일 방문 장소와 제외 카테고리 제거
→ 이름 + 주소 + 좌표 기준 중복 제거
→ 남은 후보 중 무작위 선택
~~~

서버 구현이나 검색 공급자가 바뀌어도 게임 규칙은 Android 클라이언트에서 동일하게 유지됩니다.

### 2. 지도 키와 검색 비밀 키를 분리

NAVER Map의 Dynamic Map Client ID는 빌드 시 Manifest placeholder로 주입합니다. NAVER Local Search의 Client Secret은 APK에 포함하지 않고 후보 서버에서만 관리하도록 API 경계를 설계했습니다.

후보 서버가 없는 개발 환경에서는 행정동과 테마별 검색어를 조합한 Android Geocoder 검색으로 대체합니다. 단, Geocoder 결과는 기기와 지역에 따라 달라질 수 있어 안정적인 장소 추천에는 후보 서버가 필요합니다.

### 3. GPS 오차를 고려한 연속 도착 판정

한 번의 좌표만으로 도착 처리하면 GPS 튐으로 잘못 완료될 수 있습니다. 그래서 거리와 정확도 조건을 모두 만족한 샘플이 3회 연속 들어왔을 때만 도착을 확정하고, 부정확한 샘플이 들어오면 누적 횟수를 초기화합니다.

### 4. 목적지 비공개를 UI 규칙이 아닌 상태 규칙으로 관리

도착 전에는 지도에 목적지 마커와 경로를 전달하지 않습니다. 장소명 공개 여부도 탐험 상태로 제어해 화면 변경 과정에서 목적지가 우연히 노출되는 가능성을 줄였습니다.

## 기술 스택

| 분류 | 기술 |
| --- | --- |
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose, Material 3 |
| State | ViewModel, StateFlow |
| Map | NAVER Map SDK 3.23.3 |
| Location | Android LocationManager |
| Network | HttpURLConnection, JSON |
| Local Data | SharedPreferences, JSON |
| Test | JUnit 4 |
| Build | Android Gradle Plugin 9.2.1, Gradle Kotlin DSL |

외부 의존성을 필요한 범위로 제한해 MVP의 데이터 흐름과 예외 처리를 코드에서 바로 추적할 수 있도록 했습니다.

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
~~~

## 실행 방법

### 1. 환경 준비

- Android Studio(내장 JBR 사용 권장)
- Android 12(API 31) 이상 기기 또는 에뮬레이터
- NAVER Cloud Platform Maps에 등록된 Android 앱의 Dynamic Map Client ID

### 2. 지도 Client ID 설정

프로젝트 루트의 <code>local.properties</code>에 다음 값을 추가합니다.

~~~properties
NAVER_MAP_API_KEY=발급받은_Dynamic_Map_Client_ID
~~~

### 3. 후보 서버 연결

안정적인 장소 후보 검색을 사용하려면 후보 서버의 HTTPS 기준 주소를 추가합니다.

~~~properties
RANDOM_TOUR_API_BASE_URL=https://example.com
~~~

설정하지 않거나 호출에 실패하면 Android Geocoder를 개발용 대체 검색으로 사용합니다. NAVER Local Search Client Secret은 <code>local.properties</code>나 앱 코드에 넣지 않습니다.

<details>
<summary><b>후보 API 계약 보기</b></summary>

#### Request

<code>POST /v1/explorations/candidates</code>

~~~json
{
  "latitude": 37.5445,
  "longitude": 127.0561,
  "radiusM": 1000,
  "mode": "RANDOM",
  "excludePlaceKeys": []
}
~~~

#### Response

~~~json
{
  "areaLabel": "성수동",
  "candidates": [
    {
      "placeKey": "unique-place-key",
      "name": "장소명",
      "category": "카페",
      "latitude": 37.54,
      "longitude": 127.05,
      "roadAddress": "도로명 주소"
    }
  ]
}
~~~

</details>

### 4. 빌드 및 테스트

~~~powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
~~~

생성된 APK는 <code>app/build/outputs/apk/debug/app-debug.apk</code>에서 확인할 수 있습니다.

## 검증 결과

| 검증 | 결과 |
| --- | --- |
| JVM 단위 테스트 | 핵심 도메인 테스트 4건 포함 전체 5건 통과 |
| Debug 빌드 | <code>assembleDebug</code> 성공 |
| Android Lint | 오류 0건, 의존성 버전 안내 3건 |
| 실기기 확인 | SM-S931N에서 설치·실행, 권한·설정·후보 없음 복구 흐름 확인 |

테스트 범위에는 Haversine 거리, 방위각과 방향, 3회 연속 도착 판정, 부정확한 GPS 샘플의 판정 초기화가 포함됩니다.

## 다음 단계

- NAVER Local Search 기반 후보 서버 구현 및 배포
- 탐험 진행 상태의 프로세스 종료 복원
- DataStore 또는 Room 기반 저장 계층 확장
- Compose UI 자동화 테스트와 장시간 야외 위치 추적 테스트
- 탐험 통계와 배지 등 재방문 동기 강화
