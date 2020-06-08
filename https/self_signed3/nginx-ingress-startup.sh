echo "Deploying NGINX Ingress ..."
kubectl apply -f https://raw.githubusercontent.com/nginxinc/kubernetes-ingress/v1.7.1/deployments/common/ns-and-sa.yaml
wait
kubectl apply -f https://raw.githubusercontent.com/nginxinc/kubernetes-ingress/v1.7.1/deployments/rbac/rbac.yaml
wait
kubectl apply -f https://raw.githubusercontent.com/nginxinc/kubernetes-ingress/v1.7.1/deployments/common/default-server-secret.yaml
wait
kubectl apply -f https://raw.githubusercontent.com/nginxinc/kubernetes-ingress/v1.7.1/deployments/common/nginx-config.yaml
wait
kubectl apply -f https://raw.githubusercontent.com/nginxinc/kubernetes-ingress/v1.7.1/deployments/common/vs-definition.yaml
wait
kubectl apply -f https://raw.githubusercontent.com/nginxinc/kubernetes-ingress/v1.7.1/deployments/common/vsr-definition.yaml
wait
kubectl apply -f https://raw.githubusercontent.com/nginxinc/kubernetes-ingress/v1.7.1/deployments/common/ts-definition.yaml
wait
kubectl apply -f https://raw.githubusercontent.com/nginxinc/kubernetes-ingress/v1.7.1/deployments/common/gc-definition.yaml
wait
kubectl apply -f https://raw.githubusercontent.com/nginxinc/kubernetes-ingress/v1.7.1/deployments/common/global-configuration.yaml
wait
kubectl apply -f https://raw.githubusercontent.com/nginxinc/kubernetes-ingress/v1.7.1/deployments/deployment/nginx-ingress.yaml
wait
kubectl apply -f https://raw.githubusercontent.com/nginxinc/kubernetes-ingress/v1.7.1/deployments/daemon-set/nginx-ingress.yaml
wait
kubectl apply -f https://raw.githubusercontent.com/nginxinc/kubernetes-ingress/v1.7.1/deployments/service/loadbalancer.yaml
wait
kubectl get pods --namespace=nginx-ingress
wait
kubectl get svc nginx-ingress --namespace=nginx-ingress
wait
echo "Done!"
exit 0
