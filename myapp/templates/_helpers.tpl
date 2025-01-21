{{/*
Expand the name of the chart.
*/}}
{{- define "myapp.name" -}}
{{- .Chart.Name | quote -}}
{{- end -}}

{{/*
Create a full name for the deployment and service.
*/}}
{{- define "myapp.fullname" -}}
{{- printf "%s-%s" .Release.Name .Chart.Name | quote -}}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "myapp.labels" -}}
app: {{ include "myapp.name" . }}
release: {{ .Release.Name }}
{{- end -}}