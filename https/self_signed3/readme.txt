1. attivare l'ingress deployandolo manualmente come dal sito ufficiale NGINX: https://docs.nginx.com/nginx-ingress-controller/installation/installation-with-manifests/#

cat > nginx-ingress-startup.sh << EOF
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
EOF

sudo chmod 777 nginx-ingress-startup.sh
./nginx-ingress-startup.sh

~~~~~~~~~~~~~~~~~
2. deployare i due deployment ed esporli come servizi:
kubectl create deployment web1 --image=gcr.io/google-samples/hello-app:1.0
kubectl create deployment web2 --image=gcr.io/google-samples/hello-app:2.0
kubectl expose deployment web1 --type=ClusterIP --port=80 --target-port=8080
kubectl expose deployment web2 --type=ClusterIP --port=80 --target-port=8080

~~~~~~~~~~~~~~~~~
3. creare un self signed certificate
openssl req -x509 -newkey rsa:4096 -sha256 -nodes -keyout tls.key -out tls.crt -subj "/CN=example.com" -days 365
kubectl create secret tls example-com-tls --cert=tls.crt --key=tls.key
kubectl get secret -o yaml

~~~~~~~~~~~~~~~~
4. aggiungere l'host DNS virtuale per cui abbiamo creato il certificato
echo "$(minikube ip) example.com" | sudo tee -a /etc/hosts
cat /etc/hosts | tail -1

~~~~~~~~~~~~~~~
5. applicare le ingress resource all'ingress controller gia' abilitato

cat > ingress.yaml << EOF
apiVersion: extensions/v1beta1
kind: Ingress
metadata:
  name: example-ingress
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /$1
spec:
  tls:
    - secretName: example-com-tls
      hosts:
        - example.com
  rules:
  - host: example.com
    http:
      paths:
      - path: /
        backend:
          serviceName: web1
          servicePort: 80
      - path: /v2
        backend:
          serviceName: web2
          servicePort: 80 
EOF

kubectl apply -f ingress.yaml

~~~~~~~~~~~~~~~
6. testare i servizi
curl -k https://example.com
curl -k https://example.com/v2



