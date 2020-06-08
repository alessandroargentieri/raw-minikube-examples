1. attivare l'addons ingress
minikube addons list
minikube addons enable ingress

kubectl get pods -n kube-system

~~~~~~~~~~~~~~~~~
2. deployare i due deployment ed esporli come servizi:
kubectl create deployment web1 --image=gcr.io/google-samples/hello-app:1.0
kubectl create deployment web2 --image=gcr.io/google-samples/hello-app:2.0
kubectl expose deployment web1 --type=ClusterIP --port=80 --targetPort=8080
kubectl expose deployment web2 --type=ClusterIP --port=80 --targetPort=8080

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


