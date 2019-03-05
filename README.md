# Anchorj

[![License](https://img.shields.io/badge/License-BSD%203--Clause-blue.svg)](https://opensource.org/licenses/BSD-3-Clause)
[![Build Status](https://travis-ci.org/viadee/javaAnchorExplainer.svg?branch=master)](https://travis-ci.org/viadee/javaAnchorExplainer)
[![Sonarcloud Coverage](https://sonarcloud.io/api/project_badges/measure?project=de.viadee.xai.anchor:algorithm&metric=coverage)](https://de.viadee.xai.anchor:algorithm&metric=coverage) 

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


## Why Java?
Java has been chosen as the platform's foundation, since it provides multiple advantages: 
it integrates well into a large ecosystem and can be used in conjunction with advanced technologies like H2O and 
Apache Spark. 

This implementation furthermore serves as a library based on which more approaches can be developed. 
Among others, adapters, interfaces and API's are in development to offer the opportunity of platform-independent access.

It is thus expected to reach a high dissemination among ML projects.

## Related Projects

+ This Anchors implementations features several add-ons and optional extensions which can be found in a dedicated project, called [AnchorAdapters](https://github.com/viadee/javaAnchorAdapters). These can, depending on the use-case, significantly ease implementation and customization efforts. The project aims to include methodological, i.e. default approaches to common Anchors applications. Thus, Anchors' drawback of not being application-agnostic is being approached for default domains.
+ Examples of Anchors' usage can be found in the [XAI Examples](https://github.com/viadee/xai_examples) project. It features a readily compilable Maven project that can be used to skip necessary configuration steps.


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
