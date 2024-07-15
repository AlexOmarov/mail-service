{{- define "templates.configmap_env" }}
{{- if or .Values.env .Values.global.env }}
{{- $values := deepCopy .Values | mergeOverwrite (.Values.global) }}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ include "persistence.fullname" . }}-env
  labels:
    {{- include "persistence.labels" . | nindent 4 }}
data:
{{- range $key, $value := $values.env }}
  {{ $key | quote }}: {{ $value | quote }}
{{- end }}
{{- end }}
{{- end }}
