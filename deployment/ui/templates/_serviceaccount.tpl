{{- define "templates.serviceaccount" }}
{{- if and .Values.serviceAccount .Values.serviceAccount.create -}}
apiVersion: v2
kind: ServiceAccount
metadata:
  name: {{ include "helper.serviceAccountName" . }}
  labels:
    {{- include "helper.labels" . | nindent 4 }}
  {{- with .Values.serviceAccount.annotations }}
  annotations:
    {{- toYaml . | nindent 4 }}
  {{- end }}
automountServiceAccountToken: {{ .Values.serviceAccount.automount }}
{{- end }}
{{- end }}
