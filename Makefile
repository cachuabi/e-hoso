# Quality gate for e-HoSo. scripts/check.sh, the Claude Stop hook and CI all run `make check`.
#
# RTM_ENFORCE_SPRINTS: sprints whose Must requirements must each have at least one test.
# Empty until a sprint starts being accepted; set to S1 at milestone M3 and S1,S2 at M4.
RTM_ENFORCE_SPRINTS ?=

.PHONY: check backend frontend tools rtm deps

check: tools rtm backend frontend

backend:
	cd src/backend && ./mvnw -B verify

frontend:
	cd src/frontend && npm run lint && npm run format:check && npm test && npm run build

tools:
	python3 -m unittest discover -s tools/tests

rtm:
	python3 tools/req.py rtm --enforce "$(RTM_ENFORCE_SPRINTS)"

deps:
	cd src/frontend && npm ci
	cd src/backend && ./mvnw -B -q dependency:go-offline
