{{- define "templates.service" }}
apiVersion: v1
kind: Service
metadata:
  name: {{ include "persistence.fullname" . }}
  labels:
    {{- include "persistence.labels" . | nindent 4 }}
spec:
  type: {{ .Values.service.type }}
  ports:
    - port: {{ .Values.service.port }}
      targetPort: http
      protocol: TCP
      name: http
  selector:
    {{- include "persistence.selectorLabels" . | nindent 4 }}
{{- end}}