{{- define "templates.pvc" }}

{{- if and .Values.pvc .Values.pvc.enabled -}}

apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: {{ include "helper.fullname" . }}-pvc
  labels:
    {{- include "helper.labels" . | nindent 4 }}
spec:
  storageClassName: {{ default "manual" .Values.pvc.storageClassName }}
  accessModes:
      - {{ .Values.pvc.accessMode }}
  resources:
    requests:
      storage: {{ default "1024Mi" .Values.pvc.capacity }}
  volumeName: {{ include "helper.fullname" . }}-pv

{{- end }}
{{- end }}