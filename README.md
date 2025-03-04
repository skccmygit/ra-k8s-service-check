# ra-k8s-service-check

## 개요 

본 소스코드는 ks8 클러스터에 pod 로 배포되어 배포된 클러스터 내에서의 네트워크 상태를 해당 서비스의 웹 화면을 통해 체크할 수 있는 기능을 제공한다.
대표적인 기능으로,

1. 해당 클러스터에 배포된 pod 의 IP 및 기본 정보
2. curl
3. nslookup
4. ns -vz 를 활용한 접근 테스트

와 같은 기능이 있다.

![image](https://github.com/user-attachments/assets/a07b56c9-8457-40c7-81ae-c85b30771fc1)
