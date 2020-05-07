package io.qameta.allure.owners;

import io.qameta.allure.CommonJsonAggregator;
import io.qameta.allure.CompositeAggregator;
import io.qameta.allure.Constants;
import io.qameta.allure.core.LaunchResults;
import io.qameta.allure.entity.LabelName;
import io.qameta.allure.entity.Status;
import io.qameta.allure.entity.TestResult;
import io.qameta.allure.tree.*;

import java.util.*;
import java.util.stream.Collectors;

import static io.qameta.allure.entity.LabelName.*;
import static io.qameta.allure.entity.Statistic.comparator;
import static io.qameta.allure.entity.TestResult.comparingByTimeAsc;
import static io.qameta.allure.tree.TreeUtils.calculateStatisticByChildren;
import static io.qameta.allure.tree.TreeUtils.groupByLabels;

/**
 * The plugin adds owners tab to the report.
 *
 * @since 2.0
 */
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.UseUtilityClass"})
public class OwnersPlugin extends CompositeAggregator {

    protected static final String OWNERS = "owners";

    protected static final String JSON_FILE_NAME = "owners.json";

    @SuppressWarnings("PMD.DefaultPackage")
    /* default */ static final LabelName[] LABEL_NAMES = {OWNER};

    public OwnersPlugin() {
        super(Arrays.asList(new WidgetAggregator(), new JsonAggregator()));
    }

    @SuppressWarnings("PMD.DefaultPackage")
    /* default */ static Tree<TestResult> getData(final List<LaunchResults> launchResults) {

        // @formatter:off
        final Tree<TestResult> owners = new TestResultTree(
            OWNERS,
            testResult -> groupByLabels(testResult, LABEL_NAMES)
        );
        // @formatter:on

        launchResults.stream()
            .map(LaunchResults::getResults)
            .flatMap(Collection::stream)
            .filter(testResult -> testResult.getStatus() == Status.FAILED || testResult.getStatus() == Status.BROKEN)
            .sorted(comparingByTimeAsc())
            .forEach(owners::add);
        return owners;
    }

    /**
     * Generates tree data.
     */
    private static class JsonAggregator extends CommonJsonAggregator {

        JsonAggregator() {
            super(JSON_FILE_NAME);
        }

        @Override
        protected Tree<TestResult> getData(final List<LaunchResults> launches) {
            return OwnersPlugin.getData(launches);
        }
    }

    /**
     * Generates widget data.
     */
    protected static class WidgetAggregator extends CommonJsonAggregator {

        WidgetAggregator() {
            super(Constants.WIDGETS_DIR, JSON_FILE_NAME);
        }

        @Override
        public TreeWidgetData getData(final List<LaunchResults> launches) {
            final Tree<TestResult> data = OwnersPlugin.getData(launches);
            final List<TreeWidgetItem> items = data.getChildren().stream()
                .filter(TestResultTreeGroup.class::isInstance)
                .map(TestResultTreeGroup.class::cast)
                .map(WidgetAggregator::toWidgetItem)
                .sorted(Comparator.comparing(TreeWidgetItem::getStatistic, comparator()).reversed())
                .limit(10)
                .collect(Collectors.toList());
            return new TreeWidgetData().setItems(items).setTotal(data.getChildren().size());
        }

        private static TreeWidgetItem toWidgetItem(final TestResultTreeGroup group) {
            return new TreeWidgetItem()
                .setUid(group.getUid())
                .setName(group.getName())
                .setStatistic(calculateStatisticByChildren(group));
        }
    }


}
