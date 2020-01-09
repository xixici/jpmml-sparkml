/*
 * Copyright (c) 2020 Villu Ruusmann
 *
 * This file is part of JPMML-SparkML
 *
 * JPMML-SparkML is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPMML-SparkML is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with JPMML-SparkML.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jpmml.sparkml.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.spark.ml.linalg.Matrix;
import org.apache.spark.ml.linalg.Vector;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.regression.RegressionModel;
import org.dmg.pmml.regression.RegressionModel.NormalizationMethod;
import org.dmg.pmml.regression.RegressionTable;
import org.jpmml.converter.CategoricalLabel;
import org.jpmml.converter.ContinuousLabel;
import org.jpmml.converter.Feature;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.Schema;
import org.jpmml.converter.regression.RegressionModelUtil;
import org.jpmml.sparkml.MatrixUtil;
import org.jpmml.sparkml.ModelConverter;
import org.jpmml.sparkml.VectorUtil;

public class LinearModelUtil {

	public LinearModelUtil(){
	}

	static
	public <C extends ModelConverter<?> & HasRegressionTableOptions> RegressionModel createRegression(C converter, Vector coefficients, double intercept, Schema schema){
		ContinuousLabel continuousLabel = (ContinuousLabel)schema.getLabel();

		List<Feature> features = new ArrayList<>(schema.getFeatures());
		List<Double> featureCoefficients = new ArrayList<>(VectorUtil.toList(coefficients));

		RegressionTableUtil.simplify(converter, null, features, featureCoefficients);

		return RegressionModelUtil.createRegression(features, featureCoefficients, intercept, NormalizationMethod.NONE, schema);
	}

	static
	public <C extends ModelConverter<?> & HasRegressionTableOptions> RegressionModel createBinaryLogisticClassification(C converter, Vector coefficients, double intercept, Schema schema){
		CategoricalLabel categoricalLabel = (CategoricalLabel)schema.getLabel();

		List<Feature> features = new ArrayList<>(schema.getFeatures());
		List<Double> featureCoefficients = new ArrayList<>(VectorUtil.toList(coefficients));

		RegressionTableUtil.simplify(converter, null, features, featureCoefficients);

		return RegressionModelUtil.createBinaryLogisticClassification(features, featureCoefficients, intercept, RegressionModel.NormalizationMethod.LOGIT, true, schema);
	}

	static
	public <C extends ModelConverter<?> & HasRegressionTableOptions> RegressionModel createSoftmaxClassification(C converter, Matrix coefficients, Vector intercepts, Schema schema){
		CategoricalLabel categoricalLabel = (CategoricalLabel)schema.getLabel();

		MatrixUtil.checkRows(categoricalLabel.size(), coefficients);

		List<RegressionTable> regressionTables = new ArrayList<>();

		for(int i = 0; i < categoricalLabel.size(); i++){
			Object targetCategory = categoricalLabel.getValue(i);

			List<Feature> features = new ArrayList<>(schema.getFeatures());
			List<Double> featureCoefficients = new ArrayList<>(MatrixUtil.getRow(coefficients, i));

			RegressionTableUtil.simplify(converter, targetCategory, features, featureCoefficients);

			double intercept = intercepts.apply(i);

			RegressionTable regressionTable = RegressionModelUtil.createRegressionTable(features, featureCoefficients, intercept)
				.setTargetCategory(targetCategory);

			regressionTables.add(regressionTable);
		}

		RegressionModel regressionModel = new RegressionModel(MiningFunction.CLASSIFICATION, ModelUtil.createMiningSchema(categoricalLabel), regressionTables)
			.setNormalizationMethod(RegressionModel.NormalizationMethod.SOFTMAX);

		return regressionModel;
	}
}