# PICO SBS VR Player

Galaxy S25 Ultra의 USB-C DisplayPort 출력에 연결한 PICO REAL Plus를 독립 외부 디스플레이로 사용하기 위한 Android VR 플레이어입니다. 휴대폰에는 제어 화면을 표시하고, PICO에는 SBS/TB/VR180 영상을 출력합니다.

> 이 프로젝트는 개인이 만든 비공식 프로젝트이며 PICO, ByteDance, Samsung, Google 또는 YouTube와 제휴하거나 승인을 받지 않았습니다. 제품명과 상표는 호환 대상 설명 목적으로만 사용합니다.

## 주요 기능

- PICO의 2160×3840 세로 EDID를 90° 회전된 VR 캔버스로 출력
- 좌우 SBS, 상하 3D, 일반 영상 입력
- VR180 및 평면 SBS 보정 모드
- 좌우 눈 교환, 수평 반전, 초점·시점·화면 크기 조정
- 원형 트랙패드를 이용한 시점 이동
- L/R 전체화면 테스트 패턴
- 공식 YouTube IFrame 플레이어와 공식 YouTube 앱 열기

## YouTube 지원 범위

이 공개 버전은 YouTube 영상을 다운로드·추출·변환하지 않습니다. YouTube 주소는 공식 IFrame 플레이어로 열거나 설치된 공식 YouTube 앱에 전달하는 용도로만 사용됩니다.

YouTube의 VR180/360 센서 제어와 Cardboard 기능은 공식 플레이어가 해당 영상과 기기에서 제공하는 경우에만 사용할 수 있습니다. 직접 보유하거나 이용 허락을 받은 영상은 `로컬 영상 선택`으로 재생할 수 있습니다.

## 사용 방법

1. Android Studio 또는 아래 Gradle 명령으로 APK를 빌드합니다.
2. Galaxy S25 Ultra에 APK를 설치합니다.
3. PICO REAL Plus를 USB-C DP 케이블로 연결합니다.
4. 앱 상단의 `외부 화면 연결됨` 표시를 확인합니다.
5. 먼저 `L/R 전체화면 테스트 패턴`으로 양쪽 눈 출력을 확인합니다.
6. 영상 형식과 투영 방식을 선택한 뒤 로컬 영상을 엽니다.

## 빌드

JDK 17과 Android SDK가 필요합니다.

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

APK는 `app/build/outputs/apk/debug/app-debug.apk`에 생성됩니다.

## 라이선스

프로젝트 자체 코드는 [MIT License](LICENSE)로 배포됩니다. 사용 라이브러리는 각 프로젝트의 라이선스를 따르며 자세한 내용은 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)를 참고하세요.
