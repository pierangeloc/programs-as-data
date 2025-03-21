# The power of Programs as data

In this project we showcase the expressiveness of functional domain modeling. The purpose is to privide us 
with a precise language to describe solutions to a problem.

We do this through 3 examples:

1. A healthcheck service for a microservice. Healthcheck can be as simple as responding 200OK as long as the application is running, or go more in depth to check dependencies from underlying services. Sometimes a restart is all we need so it's better to delegate this to infrastructure.
2. A rule engine to determine whether a user has access to some data. We start defining this in the domain of electric vehicles charging, and we see how we can make this more generic. We can see also how type-level tricks such as phantom types can help us constrain further the combinations to satisfy business constraints

For each problem we show there are 2 types of encoding:
- Declarative (Initial) encoding, where we use free structures
- Final encoding, where the structure comes with a specific interpretation

Each approach has its plus and minuses, however Declarative encoding makes the structure free from interpretation, and this comes with obvious benefits:
- We can provide different interpretations, e.g. one as a running program, and one to render the program in a visually convenient way
- we can optimize before blindly executing complex rules


# IDEA for the Health check
- Find a way to compute at compile-time the requirements for the interpreter, so we don't have to pass unnecessary requirements in the environment
- Implement as a mermaid
- Implement as a fully fledged laminar UI that embeds a mermaid diagram. Dynamically traverse this diagram in a step-by-step fashion, as in a debugger with fwd/bwd functionality and key bindings
- Laws: Chcck a list of 2 topics is the same as checking each topic individually and joining them through an `||`
- Show you can optimize the program before running it
- Show you can have an always aligned frontend, representing all the possible health situations