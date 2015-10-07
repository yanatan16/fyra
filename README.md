# fyra

A clojure implementation of Functional Relational Programming as introduced by Marks and Moseley in _[Out of the Tar Pit](http://shaffner.us/cs/papers/tarpit.pdf)_.

**Work in Progress**

This is in a very early stage, as I am learning how to implement a relational query engine as well as exploring the design strategies of Functional Relational Programming, as well as designing a new DSL in clojure (and the macro funkiness to go along).

Eventually, I'd like to see if this pragma can scale out to web application design, where shared definitions of relvars and views allow a react-based front-end and an RDBMS-backed server to communicate efficiently and stay in sync. All with the hopefulness of avoiding the complexity of state management.

## Documentation

Currently, you'll have to read some code.

- [defrelvar, defview, select/update/delete](https://github.com/yanatan16/fyra/blob/master/src/fyra/core.clj)
- [relational agebraic operators](https://github.com/yanatan16/fyra/blob/master/src/fyra/relational.clj)
- [test app inserting and deffing](https://github.com/yanatan16/fyra/blob/master/test/fyra/test_app.clj)
- [tests using select/update/delete with algebraic operations](https://github.com/yanatan16/fyra/blob/master/test/fyra/core_test.clj)

## Examples

The [real estate](https://github.com/yanatan16/fyra/tree/master/examples/real-estate) example from the original paper is reproduced here.

## License

See [LICENSE](https://github.com/yanatan16/fyra/blob/master/LICENSE) file.