# Booky - Exploring Datomic and PostgreSQL

A practical comparison of Datomic and PostgreSQL through building a library management system. This repository accompanies a 5-part blog series that explores how these databases handle similar challenges in different ways.

## What's Inside?

This is a learning project that implements the same library system in both Datomic and PostgreSQL. Through building real features like book borrowing, user management, and historical tracking, we explore and compare:

- Data modeling approaches
- Temporal data handling
- Advanced querying patterns
- Recursive relationships
- Production considerations

## Blog Series

1. [Datomic vs PostgreSQL: Data Modeling Fundamentals (Part 1)](https://flexiana.com/2023/08/side-by-side-datomic-and-postgresql-part-1-2) - How each database approaches schema design and basic operations
2. [Datomic vs PostgreSQL: Time Travel and Historical Data (Part 2)](https://flexiana.com/2025/02/side-by-side-datomic-and-postgresql-part-2) - Exploring Datomic's built-in temporal features vs PostgreSQL solutions
3. Datomic vs PostgreSQL: Deep Dive into Querying and Rules (Part 3) - Advanced query patterns and rule systems
4. Datomic vs PostgreSQL: Recursive and Graph Queries (Part 4) - Handling complex relationships and hierarchies
5. Datomic vs PostgreSQL: Production Architecture and Performance (Part 5) - Real-world deployment and optimization

## Prerequisites

- Clojure 1.11+
- PostgreSQL 14+
- [Datomic 1.0.6+](https://docs.datomic.com/setup/pro-setup.html#get-datomic)
