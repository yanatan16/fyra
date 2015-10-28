# fyra

A clojure implementation of Functional Relational Programming as introduced by Marks and Moseley in _[Out of the Tar Pit](http://shaffner.us/cs/papers/tarpit.pdf)_.

**TLDR** This project is basically an experiment to see if adding the Relational model to modern clojurescript web programming can improve it.

## Background

### Work in Progress

This is in a very early stage, as I am learning how to implement a relational query engine as well as exploring the design strategies of Functional Relational Programming, as well as designing a new DSL in clojure (and the macro funkiness to go along).

Eventually, I'd like to see if this paradigm can scale out to web application design, where shared definitions of relvars and views allow a react-based front-end and an RDBMS-backed server to communicate efficiently and stay in sync. All with the hopefulness of avoiding the complexity of state management.

### Functional Relational Programming (FRP')

Functional _Relational_ Programming (FRP') is not to be confused with Functional _Reactive_ Programming, which is a different paradigm.

FRP' was introduced in _[Out of the Tar Pit](http://shaffner.us/cs/papers/tarpit.pdf)_, which is worth the read if you think all software is broken. In the paper, the authors suggest this new paradigm for writing programs.

FRP' is basically the following:

- A set of relations (relvars) which comprise the _essential state_ of the system
- A set of views (relvars derived using relational algebra) which comprise the way a system will look at its _essential state_
  + These views can use _pure functions_ to arbitrarily modify the relational values
- A set of feeders, which input and change data in the system (insert/update/delete)
- A set of observers, which update output to other systems/the user upon change of a monitored relvar.
- A set of performance and optimizing "hints" that have nothing to do with the outcomes of the system

### Why I'm experimenting

After reading about this, I wondered how a system like this could fit with a react framework, such as reagent. Reagent already provides much of the "observer" mechanism by monitoring Reagent atoms for changes and updating components as necessary. Feeders can be in `on-click` handlers and the like.

But there are still issues lingering in modern clojurescript web apps, especially because state, whilst encouraged to be kept in one place, is still user managed and often stores more than it needs. Additionally, I have never encountered a server sync abstraction that didn't leak, but perhaps this project might lead me to one that won't (doubt it :/).

FRP' encourages separation of all these issues: core state, how the app interacts with state, actual parts that do interactions, and most importantly: how state is managed and stored.

So I think there might be something here. I'm probably wrong, because its been almost 10 years since the paper came out and I couldn't find any popular FRP' libraries. But who knows.


## Documentation

Currently, you'll have to read some code.

- [defrelvar, defview, select/update/delete](https://github.com/yanatan16/fyra/blob/master/src/fyra/core.clj)
- [relational agebraic operators](https://github.com/yanatan16/fyra/blob/master/src/fyra/relational.clj)
- [test app inserting and deffing](https://github.com/yanatan16/fyra/blob/master/test/fyra/test_app.clj)
- [tests using select/update/delete with algebraic operations](https://github.com/yanatan16/fyra/blob/master/test/fyra/core_test.clj)

## Examples

The [real estate](https://github.com/yanatan16/fyra/tree/master/examples/real-estate) example from the original paper is reproduced here.

## TODO

- enable the example
- add api for storage hints
- port to cljs/reagent

## License

See [LICENSE](https://github.com/yanatan16/fyra/blob/master/LICENSE) file.
