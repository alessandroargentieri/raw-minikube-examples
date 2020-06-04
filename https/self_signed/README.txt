minikube addons list
minikube addons enable ingress
kubectl get pods -n kube-system
kubectl run nginx --image=nginx
kubectl get pods
kubectl expose deployment nginx --port 80

cat > ingress.yaml << EOF
---
apiVersion: extensions/v1beta1
kind: Ingress
metadata:
  name: nginx
spec:
  rules:
    - host: example.com
      http:
        paths:
          - backend:
              serviceName: nginx
              servicePort: 80
EOF

kubectl apply -f ingress.yaml

echo "$(minikube ip) example.com" | sudo tee -a /etc/hosts
cat /etc/hosts | tail -1

curl example.com

openssl req -x509 -newkey rsa:4096 -sha256 -nodes -keyout tls.key -out tls.crt -subj "/CN=example.com" -days 365
ls 
kubectl create secret tls example-com-tls --cert=tls.crt --key=tls.key
kubectl get secret -o yaml
nano ingress.yaml

spec:
  tls:
    - secretName: example-com-tls
      hosts:
        - example.com
  rules:

kubectl apply -f ingress.yaml

curl -k https://example.com  

