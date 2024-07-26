{{- define "templates.pv" }}

{{- if and .Values.pv .Values.pv.enabled -}}

apiVersion: v1
kind: PersistentVolume
metadata:
  name: {{ include "helper.fullname" . }}-pv
  labels:
    {{- include "helper.labels" . | nindent 4 }}
spec:
  storageClassName: {{ default "manual" .Values.pv.storageClassName }}
  persistentVolumeReclaimPolicy: Retain
  capacity:
    storage: {{ default "1024Mi" .Values.pv.capacity }}
  {{- with .Values.pv.accessModes }}
  accessModes:
    {{- toYaml . | nindent 8 }}
  {{- end }}
  hostPath:
    path: {{ .Values.pv.hostPath }}

{{- end }}
{{- end }}