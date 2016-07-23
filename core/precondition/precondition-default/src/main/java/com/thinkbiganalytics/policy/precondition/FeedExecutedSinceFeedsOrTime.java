package com.thinkbiganalytics.policy.precondition;

import com.google.common.collect.Lists;
import com.thinkbiganalytics.metadata.api.sla.WithinSchedule;
import com.thinkbiganalytics.metadata.rest.model.sla.Obligation;
import com.thinkbiganalytics.metadata.sla.api.ObligationGroup;
import com.thinkbiganalytics.policy.PolicyProperty;
import com.thinkbiganalytics.policy.PolicyPropertyRef;
import com.thinkbiganalytics.policy.validation.PolicyPropertyTypes;

import org.joda.time.Period;

import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by sr186054 on 7/12/16.
 */
@PreconditionPolicy(name = PreconditionPolicyConstants.FEED_EXECUTED_SINCE_FEEDS_OR_TIME_NAME,
                    shortDescription = "Policy will trigger the feed when all of the supplied feeds have successfully finished or always at the given cron expression supplied",
                    description = "Policy will trigger the feed when all of the supplied feeds have successfully finished or always at the given cron expression supplied.  Both the Cron Expression and the Feeds input are required attributes")
public class FeedExecutedSinceFeedsOrTime extends FeedExecutedSinceFeeds {

    @PolicyProperty(name = "Cron Expression", type = PolicyPropertyTypes.PROPERTY_TYPE.cron, required = true, hint = "Supply a cron expression to indicate when this feed should run")
    private String cronExpression;


    public FeedExecutedSinceFeedsOrTime(@PolicyPropertyRef(name = "Cron Expression") String cronExpression, @PolicyPropertyRef(name = "Since Feed") String sinceCategoryAndFeedName,
                                        @PolicyPropertyRef(name = "Dependent Feeds") String categoryAndFeeds) {
        super(sinceCategoryAndFeedName, categoryAndFeeds);
        this.cronExpression = cronExpression;
    }


    @Override
    public Set<com.thinkbiganalytics.metadata.rest.model.sla.ObligationGroup> getPreconditionObligations() {
        Set<com.thinkbiganalytics.metadata.rest.model.sla.ObligationGroup> preconditionGroups = new HashSet<>();
        preconditionGroups.addAll(super.getPreconditionObligations());

        try {
            Period p = new Period(0, 0, 1, 0);
            String withinPeriod = p.toString();
            WithinSchedule metric = new WithinSchedule(cronExpression, withinPeriod);
            Obligation obligation = new Obligation();
            obligation.setMetrics(Lists.newArrayList(metric));
            com.thinkbiganalytics.metadata.rest.model.sla.ObligationGroup group = new com.thinkbiganalytics.metadata.rest.model.sla.ObligationGroup();
            group.addObligation(obligation);
            group.setCondition(ObligationGroup.Condition.SUFFICIENT.name());
            preconditionGroups.add(group);
        } catch (ParseException e) {

        }
        return preconditionGroups;
    }

}
