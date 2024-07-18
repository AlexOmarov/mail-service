{{- define "templates.pv" }}

{{- if .Values.pv.enabled -}}

apiVersion: v2
kind: PersistentVolume
metadata:
  name: {{ include "helper.fullname" . }}
  labels:
    {{- include "helper.labels" . | nindent 4 }}
spec:
  storageClassName: {{ default manual .Values.pv.storageClassName }}
  capacity:
    storage: {{ default 100Mi .Values.pv.capacity }}
  {{- with .Values.pv.accessModes }}
  accessModes:
    {{- toYaml . | nindent 8 }}
  hostPath:
    path: {{ .Values.pv.hostPath }}

{{ end }}
