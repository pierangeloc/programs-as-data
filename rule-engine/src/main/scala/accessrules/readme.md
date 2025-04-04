Here we want to write some access rules:
User  with Roles (A, B, C) and with provider X can access Location Y, otherwise not.

We want to render these rules in 3 ways:
1. As an access Engine: (Input, Resource) => Access:=T/F
2. As a search query: (Input) => extra query to view or not view something
3. As a visualization/mermaid