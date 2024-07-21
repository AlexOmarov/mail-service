{{- define "templates.configmap" }}

{{- range $map := .Values.configmaps}}
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ include "helper.fullname" $ }}-{{ $map.name }}
  labels:
    {{- include "helper.labels" $ | nindent 4 }}
data:
  {{- if eq $map.type "json" }}
  {{ $map.name }}.json: |-
  {{- toJson $map.data | nindent 4 }}
  {{- else if eq $map.type "yaml" }}
  {{ $map.name }}.yaml: |-
  {{- toYaml $map.data | nindent 4 }}
  {{- else if eq $map.type "file" }}
  {{- toYaml $map.data | nindent 2 }}
  {{- end }}
{{- end }}
{{- end }}
