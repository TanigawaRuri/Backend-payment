[한국어](README.md) | 日本語

# 決済・リワード バックエンドシステム

同時リクエスト環境におけるデータ整合性、パフォーマンス、非同期イベント処理を検証するために実装した、決済・リワードのバックエンドプロジェクトです。
並行性の問題を k6 で検証し、決済処理と通知処理を Kafka ベースの非同期構成に分離しました。

デプロイ先 : http://kyungmunkang.com/swagger-ui/index.html

## 目次

* [プロジェクト概要](#プロジェクト概要)
* [技術スタック](#技術スタック)
* [アーキテクチャ](#アーキテクチャ)
* [主な機能](#主な機能)
* [トラブルシューティング](#トラブルシューティング)
* [振り返り](#振り返り)
* [簡単な使い方](#簡単な使い方)


## プロジェクト概要

多数のリクエストが同時に発生する状況で、次の課題を検証し解決することを目標にしました。

同時リクエスト時に、数量と残高の整合性は保たれるか？\
同時リクエスト時に、API の応答性能を維持できるか？\
通知処理によって、コアとなる決済・リワード処理の速度が低下しないか？\
外部 API のリクエスト制限を安定して処理できるか？\
機能追加やコード変更を行っても、既存機能を自動で検証できるか？

- 期間: 2026-07 ~ 2026-09
- 形態: 個人プロジェクト

## 技術スタック

Backend : 
![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)
![Go](https://img.shields.io/badge/go-%2300ADD8.svg?style=for-the-badge&logo=go&logoColor=white)
![Postgres](https://img.shields.io/badge/postgres-%23316192.svg?style=for-the-badge&logo=postgresql&logoColor=white)

Messaging / Infra : 
![Apache Kafka](https://img.shields.io/badge/apachekafka-%23231F20.svg?style=for-the-badge&logo=apachekafka&logoColor=white)
![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)

Testing / CI : 
![k6](https://img.shields.io/badge/k6-7D64FF.svg?style=for-the-badge&logo=k6&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/github%20actions-%232671E5.svg?style=for-the-badge&logo=githubactions&logoColor=white)

## アーキテクチャ

![Architecture](docs/reward_platform_architecture.png)

## 主な機能

![Swagger UI](docs/swagger-ui.png)

**バックエンドコンテナ**

ユーザー : 会員登録、ログイン

リワード : リワードイベントの作成、リワードの請求、リワード履歴の照会

ウォレット : 現在の残高照会

**通知コンテナ**

Resend を利用したメール送信

## トラブルシューティング

問題 1: Reward リクエストに Optimistic Lock を使用していたが、在庫が残っているにもかかわらず付与に失敗する問題。

解決 : k6 の負荷テストで Pessimistic Lock と Atomic Update の速度を比較し、より高速だった Atomic Update を採用。

<details>
<summary>Optimistic Lock</summary>

テスト条件
- 総ユーザー数: 50
- リワード数量: 20

結果
- 成功数: 5
- 競合数: 45
- 異常エラー: 0

HTTP 指標
- 総リクエスト数: 151
- 失敗リクエスト率: 62.91%
- 平均応答時間: 37.99ms
- p95 応答時間: 71.07ms

</details>

<details>
<summary>Pessimistic Lock</summary>

テスト条件
- 総ユーザー数: 500
- リワード数量: 50

結果
- 成功数: 50
- 競合数: 450
- 異常エラー: 0

HTTP 指標
- 総リクエスト数: 1501
- 失敗リクエスト率: 63.29%
- 平均応答時間: 260.33ms
- p95 応答時間: 955.68ms

</details>

<details>
<summary>Atomic Update</summary>

テスト条件
- 総ユーザー数: 500
- リワード数量: 50

結果
- 成功数: 50
- 競合数: 450
- 異常エラー: 0

HTTP 指標
- 総リクエスト数: 1501
- 失敗リクエスト率: 63.29%
- 平均応答時間: 145.43ms
- p95 応答時間: 480.88ms

</details>

問題 2 : 通知サービスを決済トランザクションに同期的に組み込むと、処理が遅くなる現象。

解決 : Kafka を利用して通知サービスを分離。その際 Go を採用し、並行処理の制御と小規模サービスの構築を容易にした。

問題 3 : ネットワークの再送やクライアントの重複リクエストにより、同一の Reward リクエストが複数回届いた場合、1 つのリクエストが複数回処理される可能性。

解決 : Idempotency Key を使用して同一リクエストを識別し、すでに処理済みのリクエストが再度届いた場合は、既存の処理結果を返却。

問題 4 : バックエンドサービスを大きく修正するとエラーが発生するが、修正後に既存機能が壊れていないかを手動で確認するのが困難。

解決 : GitHub Actions の CI で IntegrationTest と UnitTest を自動化。その過程で、イメージ名を GitHub リポジトリ名の小文字に設定するロジックを追加。その後、CD によりビルドからデプロイまでを自動化。

問題 5 : k6 を用いた負荷テスト時に、Resend のポリシーにより短時間の大量メール送信が拒否される。

解決 : Token Bucket アルゴリズムを用いたレートリミッターを設定。

## 振り返り

Reward の同時実行問題を解決する中で、単にロックを適用するのではなく、実際のリクエストパターンとデータモデルを考慮して、適切な並行性制御の方式を選択する必要があることを確認しました。

非同期メッセージングは、サービス間の結合度を下げ、外部システムの遅延を隔離する手段として活用できることを経験しました。

今後の拡張予定は、実環境へのデプロイと、Kafka イベントへのメールアドレス情報の追加です。これにより、Resend を利用して各ユーザーが実際のメールアドレスで Reward 情報を受け取れるようにします。（完了）

また、Retry を複数回行っても失敗した場合、そのメールが消失してしまうという設計上の弱点があります。今後 DLQ のロジックを追加することで、失敗したメールを別途管理できます。

## 簡単な使い方

 1. register API で会員登録（パスワードは 8 文字以上。実在するメールアドレスを推奨 - Resend によるメール受信が可能）
 2. login API でトークンを取得し、右上の Authorization で認証
 3. 認証後、新規イベントを登録でき、既存のイベント情報は get reward-events で取得可能
 4. reward-events/claims に「存在するイベント ID」と UUID を含めて POST を実行 -> デプロイ先ドメインでログインしているメールアドレスにリワードが送信されます