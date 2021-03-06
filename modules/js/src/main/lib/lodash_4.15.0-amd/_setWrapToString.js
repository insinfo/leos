define(['./constant', './_defineProperty', './_getWrapDetails', './identity', './_insertWrapDetails', './_updateWrapDetails'], function(constant, defineProperty, getWrapDetails, identity, insertWrapDetails, updateWrapDetails) {

  /**
   * Sets the `toString` method of `wrapper` to mimic the source of `reference`
   * with wrapper details in a comment at the top of the source body.
   *
   * @private
   * @param {Function} wrapper The function to modify.
   * @param {Function} reference The reference function.
   * @param {number} bitmask The bitmask flags. See `createWrap` for more details.
   * @returns {Function} Returns `wrapper`.
   */
  var setWrapToString = !defineProperty ? identity : function(wrapper, reference, bitmask) {
    var source = (reference + '');
    return defineProperty(wrapper, 'toString', {
      'configurable': true,
      'enumerable': false,
      'value': constant(insertWrapDetails(source, updateWrapDetails(getWrapDetails(source), bitmask)))
    });
  };

  return setWrapToString;
});
