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
      targetPort: {{ .Values.service.targetPort }}
      protocol: TCP
      name: {{ .Values.service.name }}
  selector:
    {{- include "persistence.selectorLabels" . | nindent 4 }}
{{- end}}