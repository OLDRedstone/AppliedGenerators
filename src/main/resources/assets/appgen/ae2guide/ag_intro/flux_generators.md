---
navigation:
  parent: ag_intro/ag-index.md
  title: Flux Generators
  icon: appgen:flux_generator_1k
categories:
  - blocks
  - generators
item_ids:
  - appgen:flux_generator_1k
  - appgen:flux_generator_4k
  - appgen:flux_generator_16k
  - appgen:flux_generator_64k
  - appgen:flux_generator_256k
  - appgen:flux_generator_1m
  - appgen:flux_generator_4m
  - appgen:flux_generator_16m
  - appgen:flux_generator_64m
  - appgen:flux_generator_256m
---

# Flux Generators

<Column>
  <Row>
    <ItemImage id="appgen:flux_generator_1k" scale="4" />
    <ItemImage id="appgen:flux_generator_4k" scale="4" />
    <ItemImage id="appgen:flux_generator_16k" scale="4" />
    <ItemImage id="appgen:flux_generator_64k" scale="4" />
    <ItemImage id="appgen:flux_generator_256k" scale="4" />
  </Row>
  <Row>
    <ItemImage id="appgen:flux_generator_1m" scale="4" />
    <ItemImage id="appgen:flux_generator_4m" scale="4" />
    <ItemImage id="appgen:flux_generator_16m" scale="4" />
    <ItemImage id="appgen:flux_generator_64m" scale="4" />
    <ItemImage id="appgen:flux_generator_256m" scale="4" />
  </Row>
</Column>

The Flux Generators are basic devices that can generate Forge Energy permanently.

## FE Generation

The Flux Generators generate FE at the following rates.

| Flux Generator                               | FE Generation Per Tick (Default) |
|----------------------------------------------|---------------------------------:|
| <ItemLink id="appgen:flux_generator_1k" />   |                               50 |
| <ItemLink id="appgen:flux_generator_4k" />   |                              100 |
| <ItemLink id="appgen:flux_generator_16k" />  |                              200 |
| <ItemLink id="appgen:flux_generator_64k" />  |                              400 |
| <ItemLink id="appgen:flux_generator_256k" /> |                              800 |
| <ItemLink id="appgen:flux_generator_1m" />   |                            1,600 |
| <ItemLink id="appgen:flux_generator_4m" />   |                            3,200 |
| <ItemLink id="appgen:flux_generator_16m" />  |                            6,400 |
| <ItemLink id="appgen:flux_generator_64m" />  |                           12,800 |
| <ItemLink id="appgen:flux_generator_256m" /> |                           25,600 |

## Settings

The Flux Generators send FE to one of the following destinations:

- The ME System which the Flux Generator belongs to. (Default)
- The energy storages adjacent to the Flux Generator.

## Upgrades

The Singularity Generators support the following [upgrades](ae2:items-blocks-machines/upgrade_cards.md):

-   <ItemLink id="ae2:speed_card" /> increases the generation speed of the Flux Generator by +50%, for a max of +150%, or 250% of the base generation speed.
-   <ItemLink id="ae2:redstone_card" /> allows the Flux Generator to be turned on and off by redstone signals, which can be used to control the generation of FE.

(Note: Pulse-mode Flux Generators don't always produce energy on every pulse. If the pulses are fast
enough, they generate energy all at once.)
