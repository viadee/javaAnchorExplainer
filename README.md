# Anchorj

[![License](https://img.shields.io/badge/License-BSD%203--Clause-blue.svg)](https://opensource.org/licenses/BSD-3-Clause)
[![Build Status](https://travis-ci.org/viadee/javaAnchorExplainer.svg?branch=master)](https://travis-ci.org/viadee/javaAnchorExplainer)
[![Sonarcloud Coverage](https://sonarcloud.io/api/project_badges/measure?project=de.viadee:anchorj&metric=coverage)](https://de.viadee:anchorj&metric=coverage) 

This project provides an efficient java implementation of the Anchors explanation algorithm for machine learning models.

The initial proposal "Anchors: High-Precision Model-Agnostic Explanations" by Marco Tulio Ribeiro (2018) can be found
[*here*](https://homes.cs.washington.edu/~marcotcr/aaai18.pdf).


## The Algorithm

A short description of how the algorithm works is provided in the author's [*GitHub repository*](https://github.com/marcotcr/anchor/):

> An anchor explanation is a rule that sufficiently “anchors” the
prediction locally – such that changes to the rest of the feature
values of the instance do not matter. In other words, for instances on which the anchor holds, the prediction is (almost)
always the same.

> At the moment, we support explaining individual predictions for text classifiers or classifiers that act on tables (numpy arrays of numerical or categorical data). If there is enough interest, I can include code and examples for images.

> The anchor method is able to explain any black box classifier, with two or more classes. All we require is that the classifier implements a function that takes in raw text or a numpy array and outputs a prediction (integer)


## Getting Started


### Prerequisites and Installation

In order to use the core project, no prerequisites and installation is are required. 
There are no dependencies and the algorithm may be used by providing the required interfaces.


### Using the Algorithm

In order to explain a prediction, one has to use the base Anchors algorithm provided by the ``AnchorConstruction`` 
class. This class may be instantiated by using the ``AnchorConstructionBuilder``. 

Mainly, the builder requires an implementation of the ``ClassificationFunction`` and the ``PerturbationFunction``, as 
well as an instance and its label to be explained. These components must all have the same type parameter T.
The result may be built as follows:

```Java
new AnchorConstructionBuilder<>(classificationFunction, perturbationFunction, labeledInstance, instanceLabel)
        .build()
        .constructAnchor();
```

The builder offers many more options on how to construct the anchor. Amongst others, the multi-armed bandit algorithm or
the coverage calculation function may be customized. Additionally, the algorithm may be configured to utilize threading.

## Adapters

While Anchors is model-agnostic, it requires some client provided functions, e.g. perturbation functions. These can usually be defined once per domain. 
Thus, a second project, called [*javaAnchorAdapters*](https://github.com/viadee/javaAnchorAdapters), aims to include methodological, i.e. default approaches to common Anchors applications.

# Collaboration

The project is operated and further developed by the viadee Consulting AG in Münster, Westphalia. Results from theses at the WWU Münster and the FH Münster have been incorporated.
* Further theses are planned: Contact person is Dr. Frank Köhne from viadee.
    Community contributions to the project are welcome: Please open Github-Issues with suggestions (or PR), which we can then edit in the team.
*   We are looking for further partners who have interesting process data to refine our tooling as well as partners that are simply interested in a discussion about AI in the context of business process automation and explainability.

## Authors

* **Tobias Goerke** - *Initial work* - [TobiasGoerke](https://github.com/TobiasGoerke)

## License

BSD 3-Clause License

## Acknowledgments

* [*Marco Tulio Correia Ribeiro*](https://github.com/marcotcr/anchor/) for his research and a first Python implementation
