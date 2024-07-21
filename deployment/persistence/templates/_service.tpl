{{- define "helper.serviceName" -}}
{{- printf "%s-%s-service" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "templates.service" }}

apiVersion: v1
kind: Service
metadata:
  name: {{ include "helper.fullname" . }}
  labels:
    {{- include "helper.labels" . | nindent 4 }}
spec:
  type: {{ default "ClusterIP" .Values.service.type }}
  ports:
    - port: {{ .Values.service.port }}
      targetPort: {{ .Values.service.targetPort }}
      name: port
      protocol: {{ default "TCP" .Values.service.protocol }}
  selector:
    {{- include "helper.selectorLabels" . | nindent 4 }}

{{- end}}