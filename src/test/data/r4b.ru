PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#>
PREFIX :      <http://localhost/test/>

DELETE WHERE { GRAPH <http://localhost/graph/r4> {
    :r4 rdfs:label "r4" .
} }
;
INSERT DATA { GRAPH <http://localhost/graph/r4> {
    :r4 rdfs:label "r4b" .
} }
