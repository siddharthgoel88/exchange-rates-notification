//package io.sidd.rateproviders;
//
//import javax.ejb.EJB;
//
//import io.sidd.constants.RateProviderList;
//
//public class RateProviderFactory {
//	@EJB
//	static DBSRateProviderImpl dbsRateProviderImpl;
//	
//	public static RateProvider getProvider(RateProviderList provider) {
//		switch (provider) {
//		case DBS:
//			return dbsRateProviderImpl;
//
//		default:
//			return dbsRateProviderImpl;
//		}
//	}
//}
