{{- define "templates.deployment" }}
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "helper.fullname" . }}
  namespace: {{ .Release.Namespace }}
  labels:
    {{- include "helper.labels" . | nindent 4 }}
spec:
  {{- if or (not .Values.autoscaling) (not .Values.autoscaling.enabled) }}
  replicas: {{ .Values.replicaCount }}
  {{- end }}
  selector:
    matchLabels:
      {{- include "helper.selectorLabels" . | nindent 6 }}
  template:
    metadata:
      {{- with .Values.podAnnotations }}
      annotations:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      labels:
        {{- include "helper.labels" . | nindent 8 }}
        {{- with .Values.podLabels }}
        {{- toYaml . | nindent 8 }}
        {{- end }}
    spec:
      {{- with .Values.imagePullSecrets }}
      imagePullSecrets:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      serviceAccountName: {{ include "helper.serviceAccountName" . }}
      securityContext:
        {{- toYaml .Values.podSecurityContext | nindent 8 }}
      containers:
        - name: {{ .Chart.Name }}
          securityContext:
            {{- toYaml .Values.securityContext | nindent 12 }}
          image: "{{ .Values.image.repository }}:{{ .Values.image.tag | default .Chart.AppVersion }}"
          imagePullPolicy: {{ .Values.image.pullPolicy }}
          {{- if .Values.command }}
          command: {{- toYaml .Values.command | nindent 12 }}
          {{- end }}
          {{- if .Values.args }}
          args: {{- toYaml .Values.args | nindent 12 }}
          {{- end }}
          {{- if or .Values.env .Values.secretEnvName }}
          envFrom:
            {{- if .Values.env }}
            - configMapRef:
                name: {{ include "helper.fullname" $ }}-env
            {{- end }}
            {{- if .Values.secretEnvName }}
            - secretRef:
            name: {{ .Values.secretEnvName }}
            {{- end }}
          {{- end }}
          {{- with .Values.ports }}
          ports:
            {{- toYaml . | nindent 12 }}
          {{- end }}
          livenessProbe:
            {{- toYaml .Values.livenessProbe | nindent 12 }}
          readinessProbe:
            {{- toYaml .Values.readinessProbe | nindent 12 }}
          resources:
            {{- toYaml .Values.resources | nindent 12 }}
          {{- with .Values.volumeMounts }}
          volumeMounts:
            {{- toYaml . | nindent 12 }}
          {{- end }}
      {{- with .Values.volumes }}
      volumes:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      {{- with .Values.nodeSelector }}
      nodeSelector:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      {{- with .Values.affinity }}
      affinity:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      {{- with .Values.tolerations }}
      tolerations:
        {{- toYaml . | nindent 8 }}
      {{- end }}
{{- end}}