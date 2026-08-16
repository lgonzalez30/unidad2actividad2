AWS_REGION ?= us-east-1
AWS_PROFILE ?= otel-lab
AWS_ROLE_ARN ?= arn:aws:iam::070022808405:role/otel-lab-terraform-role
IMAGE_TAG ?= latest
VUS ?= 10
WARMUP ?= 30s
DURATION ?= 5m
TF_DIR := infra

.PHONY: validate build push plan deploy benchmark-baseline benchmark-otel benchmark-overhead destroy verify-cleanup service-a-url grafana-url jaeger-url

validate:
	docker compose config --quiet
	mvn -f service-a/pom.xml -B -Dmaven.repo.local=.m2/repository -DskipTests package
	mvn -f service-b/pom.xml -B -Dmaven.repo.local=.m2/repository -DskipTests package
	cd $(TF_DIR) && terraform fmt -check
	cd $(TF_DIR) && terraform init -backend=false
	cd $(TF_DIR) && terraform validate

build:
	docker build -t otel-lab-service-a:$(IMAGE_TAG) service-a
	docker build -t otel-lab-service-b:$(IMAGE_TAG) service-b
	docker build -t otel-lab-grafana:$(IMAGE_TAG) grafana-aws

push:
	@CREDS=$$(aws sts assume-role --profile $(AWS_PROFILE) --role-arn $(AWS_ROLE_ARN) --role-session-name otel-lab-ecr-push --query 'Credentials.[AccessKeyId,SecretAccessKey,SessionToken]' --output text); \
	ACCESS_KEY=$$(echo $$CREDS | awk '{print $$1}'); \
	SECRET_KEY=$$(echo $$CREDS | awk '{print $$2}'); \
	SESSION_TOKEN=$$(echo $$CREDS | awk '{print $$3}'); \
	export AWS_ACCESS_KEY_ID=$$ACCESS_KEY AWS_SECRET_ACCESS_KEY=$$SECRET_KEY AWS_SESSION_TOKEN=$$SESSION_TOKEN AWS_REGION=$(AWS_REGION); \
	ACCOUNT_ID=$$(aws sts get-caller-identity --query Account --output text); \
	aws ecr get-login-password --region $(AWS_REGION) | docker login --username AWS --password-stdin "$$ACCOUNT_ID.dkr.ecr.$(AWS_REGION).amazonaws.com"; \
	docker tag otel-lab-service-a:$(IMAGE_TAG) "$$(cd $(TF_DIR) && terraform output -raw service_a_repository_url):$(IMAGE_TAG)"; \
	docker tag otel-lab-service-b:$(IMAGE_TAG) "$$(cd $(TF_DIR) && terraform output -raw service_b_repository_url):$(IMAGE_TAG)"; \
	docker tag otel-lab-grafana:$(IMAGE_TAG) "$$(cd $(TF_DIR) && terraform output -raw grafana_repository_url):$(IMAGE_TAG)"; \
	docker push "$$(cd $(TF_DIR) && terraform output -raw service_a_repository_url):$(IMAGE_TAG)"; \
	docker push "$$(cd $(TF_DIR) && terraform output -raw service_b_repository_url):$(IMAGE_TAG)"; \
	docker push "$$(cd $(TF_DIR) && terraform output -raw grafana_repository_url):$(IMAGE_TAG)"

plan:
	cd $(TF_DIR) && AWS_PROFILE=$(AWS_PROFILE) terraform init
	cd $(TF_DIR) && terraform fmt
	cd $(TF_DIR) && AWS_PROFILE=$(AWS_PROFILE) terraform validate
	cd $(TF_DIR) && AWS_PROFILE=$(AWS_PROFILE) terraform plan -out=tfplan

deploy:
	cd $(TF_DIR) && AWS_PROFILE=$(AWS_PROFILE) terraform init
	cd $(TF_DIR) && AWS_PROFILE=$(AWS_PROFILE) terraform apply -target=aws_budgets_budget.lab -target=aws_ecr_repository.service_a -target=aws_ecr_repository.service_b -target=aws_ecr_repository.grafana -target=aws_ecr_lifecycle_policy.service_a -target=aws_ecr_lifecycle_policy.service_b -target=aws_ecr_lifecycle_policy.grafana
	$(MAKE) build
	$(MAKE) push
	cd $(TF_DIR) && AWS_PROFILE=$(AWS_PROFILE) terraform apply -var="image_tag=$(IMAGE_TAG)"

benchmark-baseline:
	OTEL_ENABLED=false docker compose up --build -d
	docker compose --profile benchmark run --rm -e VUS=$(VUS) -e WARMUP=$(WARMUP) -e DURATION=$(DURATION) k6 run --summary-trend-stats "avg,min,med,p(90),p(95),p(99),max" /scripts/load-test.js

benchmark-otel:
	OTEL_ENABLED=true docker compose up --build -d
	docker compose --profile benchmark run --rm -e VUS=$(VUS) -e WARMUP=$(WARMUP) -e DURATION=$(DURATION) k6 run --summary-trend-stats "avg,min,med,p(90),p(95),p(99),max" /scripts/load-test.js

benchmark-overhead:
	VUS=$(VUS) WARMUP=$(WARMUP) DURATION=$(DURATION) bash benchmark/run-overhead.sh

destroy:
	cd $(TF_DIR) && AWS_PROFILE=$(AWS_PROFILE) terraform destroy

service-a-url:
	@CREDS=$$(aws sts assume-role --profile $(AWS_PROFILE) --role-arn $(AWS_ROLE_ARN) --role-session-name otel-lab-service-url --query 'Credentials.[AccessKeyId,SecretAccessKey,SessionToken]' --output text); \
	ACCESS_KEY=$$(echo $$CREDS | awk '{print $$1}'); \
	SECRET_KEY=$$(echo $$CREDS | awk '{print $$2}'); \
	SESSION_TOKEN=$$(echo $$CREDS | awk '{print $$3}'); \
	export AWS_ACCESS_KEY_ID=$$ACCESS_KEY AWS_SECRET_ACCESS_KEY=$$SECRET_KEY AWS_SESSION_TOKEN=$$SESSION_TOKEN AWS_REGION=$(AWS_REGION); \
	TASK_ARN=$$(aws ecs list-tasks --cluster otel-lab-cluster --service-name otel-lab-service-a --region $(AWS_REGION) --query 'taskArns[0]' --output text); \
	ENI_ID=$$(aws ecs describe-tasks --cluster otel-lab-cluster --tasks $$TASK_ARN --region $(AWS_REGION) --query 'tasks[0].attachments[0].details[?name==`networkInterfaceId`].value' --output text); \
	PUBLIC_IP=$$(aws ec2 describe-network-interfaces --network-interface-ids $$ENI_ID --region $(AWS_REGION) --query 'NetworkInterfaces[0].Association.PublicIp' --output text); \
	echo http://$$PUBLIC_IP:8080

grafana-url:
	@CREDS=$$(aws sts assume-role --profile $(AWS_PROFILE) --role-arn $(AWS_ROLE_ARN) --role-session-name otel-lab-grafana-url --query 'Credentials.[AccessKeyId,SecretAccessKey,SessionToken]' --output text); \
	ACCESS_KEY=$$(echo $$CREDS | awk '{print $$1}'); \
	SECRET_KEY=$$(echo $$CREDS | awk '{print $$2}'); \
	SESSION_TOKEN=$$(echo $$CREDS | awk '{print $$3}'); \
	export AWS_ACCESS_KEY_ID=$$ACCESS_KEY AWS_SECRET_ACCESS_KEY=$$SECRET_KEY AWS_SESSION_TOKEN=$$SESSION_TOKEN AWS_REGION=$(AWS_REGION); \
	TASK_ARN=$$(aws ecs list-tasks --cluster otel-lab-cluster --service-name otel-lab-grafana --region $(AWS_REGION) --query 'taskArns[0]' --output text); \
	ENI_ID=$$(aws ecs describe-tasks --cluster otel-lab-cluster --tasks $$TASK_ARN --region $(AWS_REGION) --query 'tasks[0].attachments[0].details[?name==`networkInterfaceId`].value' --output text); \
	PUBLIC_IP=$$(aws ec2 describe-network-interfaces --network-interface-ids $$ENI_ID --region $(AWS_REGION) --query 'NetworkInterfaces[0].Association.PublicIp' --output text); \
	echo http://$$PUBLIC_IP:3000

jaeger-url:
	@CREDS=$$(aws sts assume-role --profile $(AWS_PROFILE) --role-arn $(AWS_ROLE_ARN) --role-session-name otel-lab-jaeger-url --query 'Credentials.[AccessKeyId,SecretAccessKey,SessionToken]' --output text); \
	ACCESS_KEY=$$(echo $$CREDS | awk '{print $$1}'); \
	SECRET_KEY=$$(echo $$CREDS | awk '{print $$2}'); \
	SESSION_TOKEN=$$(echo $$CREDS | awk '{print $$3}'); \
	export AWS_ACCESS_KEY_ID=$$ACCESS_KEY AWS_SECRET_ACCESS_KEY=$$SECRET_KEY AWS_SESSION_TOKEN=$$SESSION_TOKEN AWS_REGION=$(AWS_REGION); \
	TASK_ARN=$$(aws ecs list-tasks --cluster otel-lab-cluster --service-name otel-lab-jaeger --region $(AWS_REGION) --query 'taskArns[0]' --output text); \
	ENI_ID=$$(aws ecs describe-tasks --cluster otel-lab-cluster --tasks $$TASK_ARN --region $(AWS_REGION) --query 'tasks[0].attachments[0].details[?name==`networkInterfaceId`].value' --output text); \
	PUBLIC_IP=$$(aws ec2 describe-network-interfaces --network-interface-ids $$ENI_ID --region $(AWS_REGION) --query 'NetworkInterfaces[0].Association.PublicIp' --output text); \
	echo http://$$PUBLIC_IP:16686

verify-cleanup:
	aws ecs list-services --profile $(AWS_PROFILE) --cluster otel-lab-cluster --region $(AWS_REGION) || true
	aws ecs list-tasks --profile $(AWS_PROFILE) --cluster otel-lab-cluster --region $(AWS_REGION) || true
	aws dynamodb describe-table --profile $(AWS_PROFILE) --table-name otel-lab-products --region $(AWS_REGION) || true
	aws logs describe-log-groups --profile $(AWS_PROFILE) --log-group-name-prefix /otel-lab --region $(AWS_REGION) || true
	aws ecr describe-repositories --profile $(AWS_PROFILE) --repository-names otel-lab-service-a otel-lab-service-b otel-lab-grafana --region $(AWS_REGION) || true
